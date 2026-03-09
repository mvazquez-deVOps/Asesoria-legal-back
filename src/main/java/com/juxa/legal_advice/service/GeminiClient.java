package com.juxa.legal_advice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class GeminiClient {

    private final RestTemplate restTemplate;
    private final WebClient webClient;
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GeminiClient.class);

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String apiKey;

    public GeminiClient(RestTemplate restTemplate, WebClient.Builder webClientBuilder) {
        this.restTemplate = restTemplate;
        // Configuramos el WebClient para que apunte a la API de Google
        this.webClient = webClientBuilder.build();
    }

    /**
     * SOBRECARGA: Mantiene vivos los procesos que solo envían texto (como el Resumen o Arquitecto)
     */
    public String callGemini(String prompt) {
        return callGemini(prompt, null, null);
    }

    /**
     * MODO NORMAL CON SOPORTE MULTIMODAL (Para PDFs nativos)
     */
    public String callGemini(String prompt, String fileBase64, String mimeType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);

        List<Map<String, Object>> parts = new ArrayList<>();

        // 🌟 GOOGLE BEST PRACTICE: El documento (PDF/Imagen) va ANTES que el texto
        if (fileBase64 != null && !fileBase64.isEmpty() && mimeType != null) {
            parts.add(Map.of("inlineData", Map.of(
                    "mimeType", mimeType,
                    "data", fileBase64
            )));
        }

        // Luego agregamos el prompt
        parts.add(Map.of("text", prompt));

        // Aquí SÍ forzamos JSON para que no se rompa tu lógica de objetos
        Map<String, Object> bodyMap = Map.of(
                "contents", List.of(
                        Map.of("parts", parts)
                ),
                "generationConfig", Map.of("responseMimeType", "application/json")
        );

        try {
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(bodyMap, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(geminiApiUrl, requestEntity, String.class);
            return response.getBody();
        } catch (Exception e) {
            logger.error("Error en callGemini: {}", e.getMessage());
            throw new RuntimeException("Error en la comunicación con el motor de IA", e);
        }
    }

    /**
     * SOBRECARGA: Para el chat tipo stream original
     */
    public Flux<String> streamGemini(String prompt) {
        return streamGemini(prompt, null, null);
    }

    /**
     * MODO STREAM CON SOPORTE MULTIMODAL
     */
    public Flux<String> streamGemini(String prompt, String fileBase64, String mimeType) {
        List<Map<String, Object>> parts = new ArrayList<>();

        // 🌟 Igual que arriba, el documento va antes
        if (fileBase64 != null && !fileBase64.isEmpty() && mimeType != null) {
            parts.add(Map.of("inlineData", Map.of(
                    "mimeType", mimeType,
                    "data", fileBase64
            )));
        }

        parts.add(Map.of("text", prompt));


        Map<String, Object> bodyMap = Map.of(
                "contents", List.of(
                        Map.of("parts", parts)
                ),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "temperature", 0.0,
                        "topP", 0.8,
                        "topK", 40
                )
        );

        // La URL para streaming en Google cambia ligeramente (se añade streamGenerateContent)
        // y el parámetro alt=sse es vital para que WebClient no espere al final.
        String streamUrl = geminiApiUrl.replace("generateContent", "streamGenerateContent");

        return webClient.post()
                .uri(streamUrl + "?alt=sse&key=" + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(bodyMap)
                .retrieve()
                .bodyToFlux(String.class)
                .map(this::cleanStreamChunk); // Limpiamos el ruido de la API de Google
    }

    /**
     * Limpia el formato SSE de Google para enviar solo el texto puro al frontend
     */
    private String cleanStreamChunk(String chunk) {
        // La API de Google en modo SSE a veces envía metadatos, aquí podríamos
        // filtrar para que el frontend reciba solo el "text"
        return chunk;
    }
}