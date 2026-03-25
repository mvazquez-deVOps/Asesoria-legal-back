package com.juxa.legal_advice.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.juxa.legal_advice.config.exceptions.PlanLimitExceededException;
import com.juxa.legal_advice.config.exceptions.UnauthorizedUserException;
import com.juxa.legal_advice.dto.UserDataDTO;
import com.juxa.legal_advice.model.UserEntity;
import com.juxa.legal_advice.repository.UserRepository;
import com.juxa.legal_advice.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;///////////////////////////////////////////////////////////
import org.slf4j.LoggerFactory;/////////////////////////////////////
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AiController {

    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final AiBucketService aiBucketService;
    private final GeminiClient geminiClient;

    @Autowired
    private  UsageAuthorizationService usageAuthService;
    @Autowired
    private  UserService userService;
    private static final Logger log = LoggerFactory.getLogger(AiController.class); ////////////////


    @Autowired(required = false)
    private Storage storage;

    @PostMapping("/generate-initial-diagnosis")
    public ResponseEntity<Map<String, Object>> startDiagnosis(@RequestBody UserDataDTO userData) {
        try {
            UserEntity currentUser = userService.getCurrentAuthenticatedUser();

            // 1. Validar peaje de entrada (estimamos un texto base para el prompt de diagnóstico)
            String textoEstimado = "A".repeat(1000); // Equivale a unos 250 tokens base
            usageAuthService.validateSufficientTokens(currentUser, "Diagnóstico inicial", textoEstimado, null);

            // 2. Ejecutar la llamada a la IA
            Map<String, Object> response = geminiService.generateInitialChatResponse(userData);

            // 3. Descontar tokens reales
            if (response.containsKey("_usageMetadata")) {
                Map<String, Integer> metadata = (Map<String, Integer>) response.get("_usageMetadata");
                int totalTokensGastados = metadata.getOrDefault("totalTokens", 0);
                usageAuthService.consumeTokens(currentUser, totalTokensGastados);
            }

            return ResponseEntity.ok(response);

        } catch (PlanLimitExceededException | UnauthorizedUserException e) {
            throw e; // Dejamos que el manejador global de excepciones lo atrape
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/chat", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> chat(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam("message") String currentMessage,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam("userData") String userDataJson,
            @RequestParam("history") String historyJson) {
        try {
            UserEntity currentUser = userService.getCurrentAuthenticatedUser();

            // 1. Extraemos datos del JSON
            Map<String, Object> userDataMap = objectMapper.readValue(userDataJson,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> historyList = objectMapper.readValue(historyJson,
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});

// 2. LECTURA NATIVA O EXTRACCIÓN (Para saber de qué tamaño es el monstruo)
            String fileBase64 = null;
            String mimeType = null;
            String textoOcr = "";

            if (file != null && !file.isEmpty()) {
                String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
                if (filename.endsWith(".pdf") || filename.endsWith(".png") || filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
                    fileBase64 = java.util.Base64.getEncoder().encodeToString(file.getBytes());
                    mimeType = file.getContentType();
                    if (mimeType == null || mimeType.contains("octet-stream")) {
                        mimeType = filename.endsWith(".pdf") ? "application/pdf" : "image/jpeg";
                    }
                    // Asignamos peso para estimación de análisis visual
                    textoOcr = "A".repeat(12000);
                } else {
                    // AQUÍ ESTÁ EL CAMBIO PARA LOS DOCUMENTOS DE TEXTO (DOC, DOCX)
                 //   int availablePages = usageAuthService.getAvailableOcrPages(currentUser);
                    textoOcr  = geminiService.extractTextFromFile(file);
                }
            }

            // 3. PEAJE DE ENTRADA (La nueva validación heurística estilo ChatGPT)
            usageAuthService.validateSufficientTokens(currentUser, currentMessage, textoOcr, historyJson);

            // 4. PREPARACIÓN DEL PAYLOAD
            String directrices = aiBucketService.readTextFile("Hoja_deRita.csv");
            String contextoCarpetas = generarContextoBucket();

            Map<String, Object> payload = new HashMap<>();
            payload.put("message", currentMessage);
            payload.put("userData", userDataMap);
            payload.put("history", historyList);
            payload.put("hojaRuta", directrices);
            payload.put("estructuraCarpetas", contextoCarpetas);
            payload.put("textoOcr", textoOcr.startsWith("A".repeat(100)) ? "" : textoOcr); // Limpiamos el texto falso
            payload.put("fileBase64", fileBase64);
            payload.put("mimeType", mimeType);

            // 5. LLAMADA REAL A LA IA
            Map<String, Object> aiResponse = geminiService.processInteractiveChat(payload);

            // 6. DEDUCCIÓN EXACTA DE TOKENS (Post-respuesta)
            if (aiResponse.containsKey("_usageMetadata")) {
                Map<String, Integer> metadata = (Map<String, Integer>) aiResponse.get("_usageMetadata");
                int totalTokensGastados = metadata.getOrDefault("totalTokens", 0);
                usageAuthService.consumeTokens(currentUser, totalTokensGastados);
            }

            return ResponseEntity.ok(aiResponse);

        } catch (PlanLimitExceededException | UnauthorizedUserException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("--- [ERROR CONTROLLER] --- " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }


    @PostMapping("/architect")
    public ResponseEntity<Map<String, Object>> generateArchitectPrompt(@RequestBody Map<String, String> payload) {
        try {
            UserEntity currentUser = userService.getCurrentAuthenticatedUser();
            String intention = payload.get("intention");

            // 1. Validar peaje de entrada
            usageAuthService.validateSufficientTokens(currentUser, intention, null, null);

            // 2. Ejecutar la llamada a la IA
            Map<String, Object> response = geminiService.processArchitectIntention(intention);

            // 3. Descontar tokens reales
            if (response.containsKey("_usageMetadata")) {
                Map<String, Integer> metadata = (Map<String, Integer>) response.get("_usageMetadata");
                int totalTokensGastados = metadata.getOrDefault("totalTokens", 0);
                usageAuthService.consumeTokens(currentUser, totalTokensGastados);
            }

            return ResponseEntity.ok(response);

        } catch (PlanLimitExceededException | UnauthorizedUserException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/proxy-generate")
    public ResponseEntity<Map<String, Object>> generateProxyPrompt(@RequestBody Map<String, String> payload) {
        try {
            UserEntity currentUser = userService.getCurrentAuthenticatedUser();
            String prompt = payload.get("prompt");

            // 1. Validar peaje de entrada
            usageAuthService.validateSufficientTokens(currentUser, prompt, null, null);

            // 2. Ejecutar la llamada a la IA
            String aiResponse = geminiClient.callGemini(prompt);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(aiResponse);

            String cleanText = rootNode.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();

            // 3. Descontar tokens extrayendo la información y prepararla para el Frontend
            Map<String, Integer> usageStats = new HashMap<>();
            JsonNode usageNode = rootNode.path("usageMetadata");

            if (!usageNode.isMissingNode()) {
                int totalTokensGastados = usageNode.path("totalTokenCount").asInt(0);
                usageAuthService.consumeTokens(currentUser, totalTokensGastados);

                // Armamos el reporte detallado para el JSON de respuesta
                usageStats.put("inputTokens", usageNode.path("promptTokenCount").asInt(0));
                usageStats.put("outputTokens", usageNode.path("candidatesTokenCount").asInt(0));
                usageStats.put("cachedTokens", usageNode.path("cachedContentTokenCount").asInt(0));
                usageStats.put("totalTokens", totalTokensGastados);
            } else {
                usageStats.put("totalTokens", 0);
            }

            // 4. Construir la respuesta enviando el texto y los metadatos
            Map<String, Object> response = new HashMap<>();
            response.put("rawResponse", cleanText);
            response.put("_usageMetadata", usageStats); // <--- AHORA SÍ VIAJA AL FRONTEND

            return ResponseEntity.ok(response);

        } catch (PlanLimitExceededException | UnauthorizedUserException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    private String generarContextoBucket() {
        Map<String, String> carpetaContexto = Map.of(
                "Camara_de_Diputados/", "Acuerdos legislativos.",
                "FORMATOS/", "Plantillas procesales.",
                "Mercantil/", "Normativa mercantil.",
                "Marco-Recomendable/", "Guías doctrinales.",
                "Imprescindibles/", "Documentos críticos.",
                "Códigos_Civiles_Penales_Procedimientos/", "Códigos estatales."
        );

        StringBuilder contexto = new StringBuilder("### PROTOCOLO DE CONSULTA INTERNA (BUCKET)\n");
        carpetaContexto.forEach((carpeta, desc) -> contexto.append("- ").append(carpeta).append(": ").append(desc).append("\n"));

        try {
            Page<Blob> blobs = storage.list("asesoria-legal-bucket", Storage.BlobListOption.prefix("Códigos_Civiles_Penales_Procedimientos/"), Storage.BlobListOption.currentDirectory());
            contexto.append("   Subcarpetas estatales:\n");
            for (Blob blob : blobs.iterateAll()) {
                if (blob.isDirectory()) {
                    String nombreEstado = blob.getName().replace("Códigos_Civiles_Penales_Procedimientos/", "").replace("/", "").replace("_", " ");
                    contexto.append("   - ").append(nombreEstado).append("\n");
                }
            }
        } catch (Exception e) {}
        return contexto.toString();
    }

// En AiController.java

    //Metodo para abrir pdfs en JUXA docs
    @PostMapping(value = "/extract-text", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> extractTextFromDocument(@RequestParam("file") MultipartFile file) {
        try {
            // Misma lógica de GeminiService para leer PDFs
            String textoExtraido = geminiService.extractTextFromFile(file);

            return ResponseEntity.ok(Map.of("extractedText", textoExtraido));
        } catch (Exception e) {
            e.printStackTrace(); // Ver el error en consola de Java
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}