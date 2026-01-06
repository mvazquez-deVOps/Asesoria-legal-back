package com.juxa.legal_advice.controller;

import com.juxa.legal_advice.dto.UserDataDTO;
import com.juxa.legal_advice.service.GeminiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@CrossOrigin(origins = "https://asesoria-legal-frontend-284685729356.us-central1.run.app")
public class AiController {

    private final GeminiService geminiService;

    /**
     * Inicia el diálogo legal basado en los datos del formulario.
     * RUTA: POST /api/ai/generate-initial-diagnosis
     */
    @PostMapping("/generate-initial-diagnosis")
    public ResponseEntity<Map<String, String>> startDiagnosis(@RequestBody UserDataDTO userData) {
        String response = geminiService.generateInitialChatResponse(userData);
        return ResponseEntity.ok(Map.of("text", response));
    }

    /**
     * Maneja la conversación de seguimiento (Chat interactivo).
     * RUTA: POST /api/ai/chat
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, Object> payload) {
        // El front envía: { history, userData, currentMessage }
        String aiResponse = geminiService.processInteractiveChat(payload);

        // El front espera: { text, suggestions }
        return ResponseEntity.ok(Map.of(
                "text", aiResponse,
                "suggestions", List.of(
                        "¿Cuáles son los plazos legales?",
                        "¿Qué documentos necesito?",
                        "¿Cómo inicio la demanda?"
                )
        ));
    }
}
