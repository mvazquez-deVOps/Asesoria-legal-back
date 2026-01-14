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
        Map <String, Object> response = geminiService.generateInitialChatResponse(userData);
        return ResponseEntity.ok(response);
    }

    // CORRECCIÓN: 404
    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody Map<String, Object> payload) {
        // 1. Procesamos la respuesta de la IA (Gemini)
        // El payload ya contiene { chatHistory, userData, currentMessage }
        Map<String, Object> aiResponse = geminiService.processInteractiveChat(payload);
        String text = (String) aiResponse.get("text");

        try {
            // 2. Persistencia en Cloud SQL
            // Importante: Asegúrate que saveFromChat use 'chatHistory' internamente
            diagnosisService.saveFromChat(payload, text);
        } catch (Exception e) {
            // Log de error pero no bloqueamos la respuesta al usuario
            System.err.println("Error al persistir diagnóstico: " + e.getMessage());
        }

        // 3. Respuesta en formato AiChatResponse (lo que espera el Front)
        return ResponseEntity.ok(Map.of(
                "text", text,
                "suggestions", aiResponse.get("suggestions")

        ));
    }
    }
