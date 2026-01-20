package com.juxa.legal_advice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class GeminiClient {

    private final RestTemplate restTemplate;

    @Value("${gemini.api.url}")
    private String geminiApiUrl; // Quitar 'final' para que @Value funcione correctamente

    @Value("${gemini.api.key}")
    private String apiKey; // Quitar 'final'

    // Solo inyectamos el RestTemplate. Spring se encarga del resto.
    public GeminiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    // GeminiClient.java
    public String callGemini(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);

        // Crear el cuerpo de forma segura con un Map
        Map<String, Object> bodyMap = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                )
        );

        try {
            // Usar RestTemplate para enviar el objeto (él se encarga del JSON)
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(bodyMap, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(geminiApiUrl, bodyMap, String.class);
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Error al llamar a Gemini: " + e.getMessage(), e);
        }

    }


}