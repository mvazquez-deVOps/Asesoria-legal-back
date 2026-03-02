package com.juxa.legal_advice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.juxa.legal_advice.dto.UserDataDTO;
import com.juxa.legal_advice.model.UserEntity;
import com.juxa.legal_advice.repository.UserRepository;
import com.juxa.legal_advice.service.DiagnosisService;
import com.juxa.legal_advice.service.GeminiService;
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
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {
     //inyecciones
    private final GeminiService geminiService;
    private final DiagnosisService diagnosisService;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;


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
            UserEntity user = null;

            // 1. Buscamos al usuario de forma segura
            if (email != null && !email.isEmpty()) {
                user = userRepository.findByEmail(email).orElse(null);
            }

            // 2. EL CANDADO: Si no existe en la base de datos, lo bloqueamos elegantemente
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of(
                        "error", "Acceso denegado",
                        "message", "Debes estar registrado e iniciar sesión para usar el chat de JUXA."
                ));
            }

            // 3. Restricciones de plan
            boolean esPremium = "PREMIUM".equals(user.getSubscriptionPlan());
            boolean esProof = user.getTrialEndDate() != null && LocalDateTime.now().isBefore(user.getTrialEndDate());

            if (!esPremium && !esProof) {
                // Resetear contador si es un nuevo día
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

                // Incrementar contador para usuarios limitados
                user.setDailyMessageCount(user.getDailyMessageCount() + 1);
                userRepository.save(user);
            }
            // 2. Extracción de texto (OCR / Digital)
            String textoOcr = "";
            if (file != null && !file.isEmpty()) {
                System.out.println("--- [CONTROLLER] PROCESANDO ARCHIVO: " + file.getOriginalFilename() + " ---");
                textoOcr = geminiService.extractTextFromFile(file);
            }

            // 3. Reconstrucción del Payload (Sincronizado con GeminiService)
            Map<String, Object> payload = new HashMap<>();
            payload.put("message", currentMessage);
            payload.put("userData", userDataMap);
            payload.put("history", historyList);

            // Esta es la llave que tu GeminiService busca en la línea 144
            payload.put("contextoArchivo", textoOcr);

            // 4. Llamada al servicio
            Map<String, Object> aiResponse = geminiService.processInteractiveChat(payload);
            return ResponseEntity.ok(aiResponse);

        } catch (Exception e) {
            System.err.println("--- [ERROR CONTROLLER] --- " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/architect")
    public ResponseEntity<Map<String, Object>> generateArchitectPrompt(@RequestBody Map<String, String> payload) {
        try {
            String intention = payload.get("intention");
            if (intention == null || intention.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "La intención no puede estar vacía"));
            }

            // Llamamos al servicio que acabamos de crear en GeminiService
            Map<String, Object> response = geminiService.processArchitectIntention(intention);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("--- [ERROR CONTROLLER ARCHITECT] --- " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}