package com.juxa.legal_advice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient; // Importante para streaming
import java.time.Duration;

@Configuration
public class GeminiConfig {

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String apiKey;

    /**
     * Bean para llamadas tradicionales (JSON cerrado)
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(90))
                .build();
    }

    /**
     * NUEVO: Bean para Streaming (Efecto de escritura)
     * WebClient es asíncrono y permite que el Flux fluya al frontend
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public String geminiApiUrl() {
        return geminiApiUrl;
    }

    @Bean
    public String apiKey() {
        return apiKey;
    }
}
