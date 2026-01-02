package com.juxa.legal_advice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

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

    public String callGemini(String prompt) {
        // Limpieza básica del prompt para evitar errores de formato JSON
        String escapedPrompt = prompt.replace("\"", "\\\"").replace("\n", "\\n");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Credencial correcta para Google Gemini
        headers.set("x-goog-api-key", apiKey);

        String body = "{ \"contents\": [{ \"parts\": [{ \"text\": \"" + escapedPrompt + "\" }] }] }";

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    geminiApiUrl, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                return "Error del servidor: " + response.getStatusCode();
            }
        } catch (Exception e) {
            // Esto te dirá exactamente qué falló en la consola
            System.err.println("Fallo crítico en GeminiClient: " + e.getMessage());
            return "Error de conexión: " + e.getMessage();
        }
    }
}