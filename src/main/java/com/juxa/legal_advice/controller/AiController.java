package com.juxa.legal_advice.controller;

import com.juxa.legal_advice.dto.UserDataDTO;
import com.juxa.legal_advice.service.DiagnosisService;
import com.juxa.legal_advice.service.GeminiService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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



    @PostMapping(value = "/chat", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> chat(
            @RequestParam("message") String currentMessage,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam("userData") String userDataJson,
            @RequestParam("history") String historyJson) {
        try {
            // 1. Extraer texto del archivo si existe
            String contextoArchivo = "";
            if (file != null && !file.isEmpty()) {
                contextoArchivo = geminiService.extractTextFromFile(file);
            }

            // 2. Reconstruir el payload para procesar (puedes convertir los JSON Strings a Mapas aquí)
            Map<String, Object> payload = new HashMap<>();
            payload.put("message", contextoArchivo.isEmpty() ? currentMessage : "Contexto del archivo: " + contextoArchivo + "\n\nPregunta: " + currentMessage);

            // 3. Procesar con Gemini como ya lo haces
            Map<String, Object> aiResponse = geminiService.processInteractiveChat(payload);

            // ... (resto de tu lógica de persistencia asíncrona)

            return ResponseEntity.ok(aiResponse);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}