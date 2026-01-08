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

    @Value("${gcp.project-id}")
    private String projectId;

    @Value("${gcp.bucket-name}")
    private String bucketName;

    /**
     * Respuesta inicial: Aquí le damos TODO el contexto del formulario a la IA.
     */
    public String generateInitialChatResponse(UserDataDTO userData) {
        String prompt = String.format(
                "Eres el Chief Growth Officer y Director Jurídico de Duarte-Aupart Abogados (JUXA). " +
                        "Analiza el siguiente caso para el cliente %s:\n" +
                        "- Materia: %s (%s)\n" +
                        "- Ubicación: %s\n" +
                        "- Hechos relatados: %s\n\n" +
                        "Da un diagnóstico técnico inicial breve, autoritativo y resolutivo.",
                userData.getName(),
                userData.getCategory(),
                userData.getSubcategory(),
                userData.getLocation(),
                userData.getDescription() // Este es el "Me quiero divorciar"
        );

        String fullResponse = geminiClient.callGemini(prompt);
        return extractTextFromResponse(fullResponse);
    }

    /**
     * Chat Interactivo: CORRECCIÓN para no perder el contexto.
     */
    public String processInteractiveChat(Map<String, Object> payload) {
        // Extraemos el mensaje actual Y los datos del usuario que vienen en el payload
        String currentMessage = (String) payload.get("message");
        Map<String, Object> userData = (Map<String, Object>) payload.get("userData");

        // Creamos un prompt que le recuerde a la IA de qué estamos hablando
        String prompt = String.format(
                "Contexto del Cliente: %s está consultando sobre %s. " +
                        "Hechos iniciales: %s. " +
                        "Pregunta actual del cliente: %s. " +
                        "Responde como abogado senior de JUXA de forma táctica.",
                userData.get("name"),
                userData.get("subcategory"),
                userData.get("description"),
                currentMessage
        );

        String fullResponse = geminiClient.callGemini(prompt);
        return extractTextFromResponse(fullResponse);
    }

    public String generateLegalSummary(DiagnosisEntity entity) {
        String prompt = """
            Actúa como un abogado senior de JUXA. Genera un 'PLAN DE ACCIÓN JURÍDICA' profesional.
            Materia: %s. Hechos: %s.
            Divide tu respuesta exactamente en: 
            1. RESUMEN, 2. FUNDAMENTACIÓN, 3. ACCIONES, 4. PROCEDIMIENTO, 5. RECOMENDACIÓN.
            """.formatted(entity.getCategory(), entity.getDescription());

        String fullResponse = geminiClient.callGemini(prompt);
        return extractTextFromResponse(fullResponse);
    }

    private String extractTextFromResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            return root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();
        } catch (Exception e) {
            // Log para debug
            System.err.println("Error parseando JSON de Gemini: " + response);
            return "Lo siento, tuve un problema al procesar tu consulta legal. Por favor, intenta de nuevo.";
        }
    }
}