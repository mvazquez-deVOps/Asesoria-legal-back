package com.juxa.legal_advice.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class GeminiConfig {

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(10)) // No añadir set por la última versión de SpringBoot
                .readTimeout(Duration.ofSeconds(90))    //
                .build();
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
