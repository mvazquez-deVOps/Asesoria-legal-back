package com.juxa.legal_advice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.juxa.legal_advice.dto.UserDataDTO;
import com.juxa.legal_advice.model.UserEntity;
import com.juxa.legal_advice.repository.UserRepository;
import com.juxa.legal_advice.service.AiBucketService;
import com.juxa.legal_advice.service.GeminiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final AiBucketService aiBucketService; // inyección del bucket
    private final Storage storage;

    @PostMapping("/generate-initial-diagnosis")
    public ResponseEntity<Map<String, Object>> startDiagnosis(@RequestBody UserDataDTO userData) {
        Map<String, Object> response = geminiService.generateInitialChatResponse(userData);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/chat", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> chat(
            @RequestParam("message") String currentMessage,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam("userData") String userDataJson,
            @RequestParam("history") String historyJson) {
        try {
            // 1. Parseo de metadatos
            Map<String, Object> userDataMap = objectMapper.readValue(userDataJson,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> historyList = objectMapper.readValue(historyJson,
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});

            String email = (String) userDataMap.get("email");
            UserEntity user = userRepository.findByEmail(email).orElseThrow();

            // Restricciones de plan
            boolean esPremium = "PREMIUM".equals(user.getSubscriptionPlan());
            boolean esProof = user.getTrialEndDate() != null && LocalDateTime.now().isBefore(user.getTrialEndDate());
            if (!esPremium && !esProof) {
                if (user.getLastMessageDate() == null || !user.getLastMessageDate().equals(LocalDate.now())) {
                    user.setDailyMessageCount(0);
                    user.setLastMessageDate(LocalDate.now());
                }
                if (user.getDailyMessageCount() >= 10) {
                    return ResponseEntity.status(429).body(Map.of(
                            "error", "Límite diario alcanzado",
                            "message", "Has agotado tus 10 interacciones gratuitas de hoy. ¡Pásate a Premium!"
                    ));
                }
                user.setDailyMessageCount(user.getDailyMessageCount() + 1);
                userRepository.save(user);
            }

            // 2. Extracción de texto (OCR / Digital)
            String textoOcr = "";
            if (file != null && !file.isEmpty()) {
                System.out.println("--- [CONTROLLER] PROCESANDO ARCHIVO: " + file.getOriginalFilename() + " ---");
                textoOcr = geminiService.extractTextFromFile(file);
            }

            // 3. Lectura del bucket (ejemplo: Hoja_deRita.csv)
            String directrices = aiBucketService.readTextFile("Hoja_deRita.csv");
            String contextoCarpetas = generarContextoBucket();
            String contextoBucket = directrices + "\n \n" + contextoCarpetas;
            System.out.println("--- [CONTROLLER] CONTEXTO DEL BUCKET ---\n" + contextoBucket);
            // 4. Reconstrucción del Payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("message", currentMessage);
            payload.put("userData", userDataMap);
            payload.put("history", historyList);

            // Se pasa tanto el archivo subido como el bucket
            payload.put("contextoArchivo", !textoOcr.isEmpty() ? textoOcr : contextoBucket);

            // 5. Llamada al servicio Gemini
            Map<String, Object> aiResponse = geminiService.processInteractiveChat(payload);
            return ResponseEntity.ok(aiResponse);

        } catch (Exception e) {
            System.err.println("--- [ERROR CONTROLLER] --- " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }


    // Método auxiliar para generar el bloque de protocolo
    private String generarContextoBucket() {
        Map<String, String> carpetaContexto = Map.of(
                "Camara_de_Diputados/", "Acuerdos legislativos y convocatorias solemnes.",
                "FORMATOS/", "Plantillas procesales para redactar escritos.",
                "Mercantil/", "Normativa mercantil y títulos de crédito.",
                "Marco-Recomendable/", "Guías doctrinales y criterios recomendados.",
                "Imprescindibles/", "Documentos críticos de referencia (amparos, sentencias, UNESCO, pueblos indígenas, ética).",
                "Códigos_Civiles_Penales_Procedimientos/", "Códigos civiles, penales y procesales de los estados de México."
        );

        StringBuilder contexto = new StringBuilder("### PROTOCOLO DE CONSULTA INTERNA (BUCKET)\n");
        carpetaContexto.forEach((carpeta, descripcion) ->
                contexto.append("- ").append(carpeta).append(": ").append(descripcion).append("\n")
        );

        // Recorrer subcarpetas de Códigos_Civiles_Penales_Procedimientos
        Page<Blob> blobs = storage.list(
                "asesoria-legal-bucket",
                Storage.BlobListOption.prefix("Códigos_Civiles_Penales_Procedimientos/"),
                Storage.BlobListOption.currentDirectory()
        );

        contexto.append("   Subcarpetas estatales:\n");
        for (Blob blob : blobs.iterateAll()) {
            if (blob.isDirectory()) {
                String nombreEstado = blob.getName()
                        .replace("Códigos_Civiles_Penales_Procedimientos/", "")
                        .replace("/", "")
                        .replace("_", " ");
                contexto.append("   - ").append(nombreEstado)
                        .append(" → códigos civiles, penales y procesales de ")
                        .append(nombreEstado).append("\n");
            }
        }

        return contexto.toString();
    }
}