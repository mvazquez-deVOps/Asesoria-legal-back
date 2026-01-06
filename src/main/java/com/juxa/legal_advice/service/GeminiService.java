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

    @Value("${gcp.asesoria-legal-juxa-83a12}")
    private String projectId;

    @Value("${gcp.asesoria-legal-bucket}")
    private String bucketName;


    /**
     * Resuelve el error en AiController
     */
    public String generateInitialChatResponse(UserDataDTO userData) {
        String prompt = String.format(
                "Actúa como un abogado experto. Saluda a %s. El caso es de materia %s sobre %s. " +
                        "Descripción: %s. Da una primera impresión técnica breve.",
                userData.getName(), userData.getCategory(),
                userData.getSubcategory(), userData.getDescription()
        );

        String fullResponse = geminiClient.callGemini(prompt);
        return extractTextFromResponse(fullResponse);
    }

    /**
     * Resuelve el error en AiController
     */
    public String processInteractiveChat(Map<String, Object> payload) {
        String currentMessage = (String) payload.get("currentMessage");

        String prompt = "Eres el asistente legal de JUXA. Responde profesionalmente: " + currentMessage;

        String fullResponse = geminiClient.callGemini(prompt);
        return extractTextFromResponse(fullResponse);
    }

    /**
     * Usado para generar el dictamen del PDF
     */
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

    /**
     * Limpia el JSON de Gemini para que no salga en el PDF ni en el Chat
     */
    private String extractTextFromResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            return root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();
        } catch (Exception e) {
            return "Error al procesar respuesta legal: " + e.getMessage();
        }
    }
}