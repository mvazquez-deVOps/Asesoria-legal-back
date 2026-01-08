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
    public ResponseEntity<Map<String, String>> startDiagnosis(@RequestBody UserDataDTO userData) {
        String response = geminiService.generateInitialChatResponse(userData);
        return ResponseEntity.ok(Map.of("text", response));
    }

    // CORRECCIÓN: ¡Faltaba esta anotación! Por eso daba 404
    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody Map<String, Object> payload) {
        String aiResponse = geminiService.processInteractiveChat(payload);

        try {
            diagnosisService.saveFromChat(payload, aiResponse);
        } catch (Exception e) {
            System.err.println("Error al persistir: " + e.getMessage());
        }

        return ResponseEntity.ok(Map.of(
                "text", aiResponse,
                "suggestions", List.of("¿Cuáles son los plazos?", "¿Qué documentos necesito?")
        ));
    }
}