package com.juxa.legal_advice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.juxa.legal_advice.dto.UserDataDTO;
import com.juxa.legal_advice.model.DiagnosisEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GeminiService {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> generateInitialChatResponse(UserDataDTO userData) {
        String contextoPersona = "el ciudadano";
        if ("MORAL".equalsIgnoreCase(userData.getUserType())) {
            contextoPersona = "el representante de la empresa";
        }

        String descripcion = userData.getDescription() != null ? userData.getDescription() : "";
        boolean contextoPobre = descripcion.length() <= 50;

        String prompt = String.format(
                "Eres abogado senior de JUXA. Analiza el caso de %s (%s): %s. " +
                        (contextoPobre
                                ? "Genera un diagnóstico inicial breve y 3 preguntas relevantes para guiar al cliente. "
                                : "Da un diagnóstico inicial breve y sugiere 3 preguntas específicas que el cliente podría hacer. ") +
                        "Responde únicamente en formato JSON válido con tres campos:\n" +
                        "{\n" +
                        "  \"diagnosis\": \"dictamen breve y técnico (máx 150 caracteres)\",\n" +
                        "  \"suggestions\": [\"pregunta 1\", \"pregunta 2\", \"pregunta 3\"],\n" +
                        "  \"downloadPdf\": true\n" +
                        "}\n",
                userData.getName(),
                contextoPersona,
                descripcion
        );

        String fullResponse = geminiClient.callGemini(prompt);
        Map<String, Object> result = extractStructuredResponse(fullResponse);

        // Fallback: recortar diagnosis si es muy largo
        String text = (String) result.get("text");
        if (text != null && text.length() > 150) {
            result.put("text", text.substring(0, 150) + "...");
        }

        return result;
    }

    public Map<String, Object> processInteractiveChat(Map<String, Object> payload) {
        String currentMessage = (String) payload.get("message");
        Map<String, Object> userData = (Map<String, Object>) payload.get("userData");

        String descripcion = userData != null ? (String) userData.get("description") : "";
        boolean contextoPobre = descripcion.length() <= 50;

        String prompt = String.format(
                "Contexto del Cliente: %s. Hechos: %s. Pregunta actual: %s. " +
                        "Responde como abogado senior de JUXA. " +
                        (contextoPobre
                                ? "Genera 3 preguntas relevantes para guiar al cliente. "
                                : "Además, sugiere 3 preguntas que el cliente podría hacer. ") +
                        "Responde únicamente en formato JSON válido con tres campos:\n" +
                        "{\n" +
                        "  \"diagnosis\": \"dictamen breve y técnico (máx 150 caracteres)\",\n" +
                        "  \"suggestions\": [\"pregunta 1\", \"pregunta 2\", \"pregunta 3\"],\n" +
                        "  \"downloadPdf\": true\n" +
                        "}\n" +
                        "Cuando el dictamen esté listo para descarga, marca \"downloadPdf\": true.\n" +
                        "Si aún no está listo, marca \"downloadPdf\": false.\n",
                userData != null ? userData.get("name") : "Cliente",
                descripcion,
                currentMessage
        );
        String fullResponse = geminiClient.callGemini(prompt);
        Map<String, Object> result = extractStructuredResponse(fullResponse);

        String text = (String) result.get("text");
        if (text != null && text.length() > 150) {
            result.put("text", text.substring(0, 150) + "...");
        }

        return result;
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

    // Nuevo extractor estructurado: diagnosis + suggestions
    private Map<String, Object> extractStructuredResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode textNode = root.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text");

            String rawText = textNode.asText();

            if (rawText.trim().startsWith("{")) {
                JsonNode parsed = objectMapper.readTree(rawText);

                String diagnosis = parsed.path("diagnosis").asText();
                List<String> suggestions = new ArrayList<>();
                parsed.path("suggestions").forEach(node -> suggestions.add(node.asText()));

                Map<String, Object> result = new HashMap<>();
                result.put("text", diagnosis);
                result.put("suggestions", suggestions);
                return result;
            }

            return Map.of("text", rawText, "suggestions", List.of());
        } catch (Exception e) {
            return Map.of("text", "Lo siento, hubo un error procesando tu consulta.", "suggestions", List.of());
        }
    }

    // Extractor simple para casos como generateLegalSummary
    private String extractTextFromResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            return root.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();
        } catch (Exception e) {
            return "Lo siento, hubo un error procesando tu consulta.";
        }
    }
}
