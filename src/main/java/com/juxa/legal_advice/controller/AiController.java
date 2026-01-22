package com.juxa.legal_advice.controller;

import com.juxa.legal_advice.dto.UserDataDTO;
import com.juxa.legal_advice.service.DiagnosisService;
import com.juxa.legal_advice.service.GeminiService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final GeminiService geminiService;
    private final DiagnosisService diagnosisService;

    @PostMapping("/generate-initial-diagnosis")
    public ResponseEntity<Map<String, Object>> startDiagnosis(@RequestBody UserDataDTO userData) {
        Map<String, Object> response = geminiService.generateInitialChatResponse(userData);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/chat-simple")
    public ResponseEntity<Map<String, Object>> chatSimple(@RequestBody Map<String, String> body) {
        try {
            String prompt = body.get("prompt");
            String aiResponse = geminiService.callGemini(prompt);

            return ResponseEntity.ok(Map.of(
                    "text", aiResponse,
                    "suggestions", List.of(),
                    "downloadPdf", false
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "No se pudo procesar la consulta con Gemini",
                    "details", e.getMessage()
            ));
        }
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, Object> payload) {
        try {
            // 1. Procesamos la respuesta de la IA (Gemini)
            Map<String, Object> aiResponse = geminiService.processInteractiveChat(payload);
            String aiText = (String) aiResponse.get("text");
            List<String> aiSuggestions = (List<String>) aiResponse.get("suggestions");
            Boolean downloadPdf = (Boolean) aiResponse.getOrDefault("downloadPdf", false);

            try {
                // 2. Persistencia en Cloud SQL
                diagnosisService.saveFromChat(payload, aiText);
            } catch (Exception e) {
                System.err.println("Error al persistir diagnóstico: " + e.getMessage());
            }

            // 3. Respuesta en formato AiChatResponse (lo que espera el Front)
            return ResponseEntity.ok(Map.of(
                    "text", aiText,
                    "suggestions", aiSuggestions,
                    "downloadPdf", downloadPdf
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "No se pudo procesar la consulta con Gemini",
                    "details", e.getMessage()
            ));
        }
    }
}
