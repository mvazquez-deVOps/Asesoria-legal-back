package com.juxa.legal_advice.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.AccessToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
        import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Recover;
import org.springframework.web.client.HttpServerErrorException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class GeminiClient {

    private final RestTemplate restTemplate;
    private final WebClient webClient;
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GeminiClient.class);

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    public GeminiClient(RestTemplate restTemplate, WebClient.Builder webClientBuilder) {
        this.restTemplate = restTemplate;
        this.webClient = webClientBuilder.build();
    }

    /**
     * Obtiene el token de acceso dinámico de la cuenta de servicio de Cloud Run.
     * Reemplaza la necesidad de usar una API Key estática.
     */
    private String getAccessToken() throws IOException {
        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault()
                .createScoped(Collections.singleton("https://www.googleapis.com/auth/cloud-platform"));
        credentials.refreshIfExpired();
        AccessToken token = credentials.refreshAccessToken();
        return token.getTokenValue();
    }

    @Retryable(
            value = {HttpServerErrorException.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public String callGemini(String prompt, String fileBase64, String mimeType) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            // CAMBIO CLAVE: Usamos Bearer Token en lugar de x-goog-api-key
            headers.setBearerAuth(getAccessToken());

            List<Map<String, Object>> parts = new ArrayList<>();
            if (fileBase64 != null && !fileBase64.isEmpty() && mimeType != null) {
                parts.add(Map.of("inlineData", Map.of("mimeType", mimeType, "data", fileBase64)));
            }
            parts.add(Map.of("text", prompt));

            Map<String, Object> bodyMap = Map.of(
                    "contents", List.of(Map.of("role", "user","parts", parts)),
                    "generationConfig", Map.of("responseMimeType", "application/json")
            );

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(bodyMap, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(geminiApiUrl, requestEntity, String.class);
            return response.getBody();

        } catch (Exception e) {
            logger.error("Error en callGemini: {}", e.getMessage());
            throw new RuntimeException("Error en la comunicación con Vertex AI", e);
        }
    }

    public Flux<String> streamGemini(String prompt, String fileBase64, String mimeType) {
        try {
            String token = getAccessToken();
            List<Map<String, Object>> parts = new ArrayList<>();

            if (fileBase64 != null && !fileBase64.isEmpty() && mimeType != null) {
                parts.add(Map.of("inlineData", Map.of("mimeType", mimeType, "data", fileBase64)));
            }
            parts.add(Map.of("text", prompt));

            Map<String, Object> bodyMap = Map.of(
                    "contents", List.of(Map.of("role", "user","parts", parts)),
                    "generationConfig", Map.of(
                            "responseMimeType", "application/json",
                            "temperature", 0.0)
            );

            // En Vertex AI el streaming se maneja igual pero con la URL de stream
            String streamUrl = geminiApiUrl.replace(":generateContent", ":streamGenerateContent");

            return webClient.post()
                    .uri(streamUrl)
                    .headers(h -> h.setBearerAuth(token)) // Autenticación Bearer
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(bodyMap)
                    .retrieve()
                    .bodyToFlux(String.class);

        } catch (Exception e) {
            return Flux.error(new RuntimeException("Error al iniciar stream con Vertex AI", e));
        }
    }

    // Sobrecargas y métodos de limpieza se mantienen igual...
    public String callGemini(String prompt) { return callGemini(prompt, null, null); }
    public Flux<String> streamGemini(String prompt) { return streamGemini(prompt, null, null); }

    @Recover
    public String recover(HttpServerErrorException e, String prompt, String fileBase64, String mimeType) {
        logger.error("Agotados los reintentos para Vertex AI: {}", e.getMessage());
        return "{\"error\":\"Servicio saturado o no disponible\"}";
    }
}