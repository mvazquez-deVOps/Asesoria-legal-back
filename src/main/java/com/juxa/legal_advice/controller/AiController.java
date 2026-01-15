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
// CORRECCIÓN: Agregué la URL de tu sitio en Firebase para evitar errores de CORS
@CrossOrigin(origins = {
        "https://asesoria-legal-juxa-83a12.web.app",
        "https://asesoria-legal-juxa-83a12.firebaseapp.com"
})
public class AiController {

    private final GeminiService geminiService;
    private final DiagnosisService diagnosisService;

    @PostMapping("/generate-initial-diagnosis")
    public ResponseEntity<Map<String, Object>> startDiagnosis(@RequestBody UserDataDTO userData) {
        Map<String, Object> response = geminiService.generateInitialChatResponse(userData);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, Object> payload) {
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
    }
}
