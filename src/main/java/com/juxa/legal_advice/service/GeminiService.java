package com.juxa.legal_advice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.juxa.legal_advice.dto.UserDataDTO;
import com.juxa.legal_advice.model.DiagnosisEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GeminiService {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateInitialChatResponse(UserDataDTO userData) {
        String prompt = String.format(
                "Eres el Chief Growth Officer y Director Jurídico de JUXA. " +
                        "Analiza el siguiente caso para el cliente %s:\n" +
                        "- Hechos relatados: %s\n\n" +
                        "Da un diagnóstico técnico inicial breve y resolutivo.",
                userData.getName(),
                userData.getDescription()
        );

        String fullResponse = geminiClient.callGemini(prompt);
        return extractTextFromResponse(fullResponse);
    }

    public String processInteractiveChat(Map<String, Object> payload) {
        String currentMessage = (String) payload.get("message");
        Map<String, Object> userData = (Map<String, Object>) payload.get("userData");

        String prompt = String.format(
                "Contexto del Cliente: %s. Hechos: %s. Pregunta actual: %s. " +
                        "Responde como abogado senior de JUXA.",
                userData != null ? userData.get("name") : "Cliente",
                userData != null ? userData.get("description") : "No disponible",
                currentMessage
        );

        String fullResponse = geminiClient.callGemini(prompt);
        return extractTextFromResponse(fullResponse);
    }

    public String generateLegalSummary(DiagnosisEntity entity) {
        String hechos = (entity.getDescription() != null) ? entity.getDescription() : "Caso por chat";
        String contexto = (entity.getHistory() != null) ? entity.getHistory() : "Sin historial";

        String prompt = """
        Actúa como un abogado senior de JUXA. Genera un 'PLAN DE ACCIÓN JURÍDICA' profesional.
        HECHOS: %s. HISTORIAL: %s.
        Divide en: 1. RESUMEN, 2. FUNDAMENTACIÓN, 3. ACCIONES, 4. PROCEDIMIENTO, 5. RECOMENDACIÓN.
        """.formatted(hechos, contexto);

        String fullResponse = geminiClient.callGemini(prompt);
        return extractTextFromResponse(fullResponse);
    }

    private String extractTextFromResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            return root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
        } catch (Exception e) {
            return "Lo siento, hubo un error procesando tu consulta.";
        }
    }
}