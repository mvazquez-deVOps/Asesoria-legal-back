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
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GeminiClient.class);

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String apiKey;

    public GeminiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String callGemini(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);

        // CONFIGURACIÓN CRÍTICA: Forzamos el responseMimeType a application/json
        // Esto evita que Gemini envíe texto como "Aquí tienes tu JSON:" y cause errores de formato
        Map<String, Object> generationConfig = Map.of(
                "responseMimeType", "application/json"
        );

        Map<String, Object> bodyMap = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", generationConfig
        );

        try {
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(bodyMap, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(geminiApiUrl, requestEntity, String.class);
            return response.getBody();
        } catch (Exception e) {
            logger.error("Error al llamar a Gemini: {}", e.getMessage());
            throw new RuntimeException("Error en la comunicación con el motor de IA", e);
        }
    }
}