package com.juxa.legal_advice.service;

import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class GeminiClient {

    private final RestTemplate restTemplate;
    private final String geminiApiUrl;
    private final String apiKey;

    public GeminiClient(RestTemplate restTemplate,
                        String geminiApiUrl,
                        String apiKey) {
        this.restTemplate = restTemplate;
        this.geminiApiUrl = geminiApiUrl;
        this.apiKey = apiKey;
    }

    /**
     * Llama a la API de Gemini con un prompt legal y devuelve el texto generado.
     */
    public String callGemini(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        String body = """
            {
              "contents": [{
                "role": "user",
                "parts": [{"text": "%s"}]
              }]
            }
            """.formatted(prompt);

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                geminiApiUrl, request, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody(); // Aquí puedes parsear JSON si quieres extraer solo el texto
        } else {
            throw new RuntimeException("Error al llamar a Gemini: " + response.getStatusCode());
        }
    }
}