package com.juxa.legal_advice.service;

import com.juxa.legal_advice.dto.UserDataDTO; // Importante añadir este
import com.juxa.legal_advice.model.DiagnosisEntity;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GeminiService {

    private final GeminiClient geminiClient;

    /**
     * Genera un resumen legal estático (usado al guardar el diagnóstico)
     */
    public String generateLegalSummary(DiagnosisEntity entity) {
        String prompt = buildPrompt(entity);
        return geminiClient.callGemini(prompt);
    }

    /**
     * SUGERENCIA PASO 2: Genera la respuesta inicial del chat interactivo
     */
    public String generateInitialChatResponse(UserDataDTO userData) {
        String prompt = String.format(
                "Actúa como un abogado experto. El cliente ha presentado un caso de %s sobre %s. " +
                        "Descripción: %s. Salúdalo por su nombre (%s) y dale una primera impresión técnica muy breve.",
                userData.getCategory(), userData.getSubcategory(), userData.getDescription(), userData.getName()
        );

        return geminiClient.callGemini(prompt);
    }

    /**
     * SUGERENCIA PASO 2: Procesa mensajes de seguimiento en el chat
     */
    public String processInteractiveChat(Map<String, Object> payload) {
        String currentMessage = (String) payload.get("currentMessage");

        // Aquí construimos un prompt que le da el "rol" de abogado a Gemini para el chat
        String prompt = "Contexto: Eres un asistente legal inteligente. " +
                "Responde a la siguiente duda del cliente de forma profesional y clara: " + currentMessage;

        return geminiClient.callGemini(prompt);
    }

    private String buildPrompt(DiagnosisEntity entity) {
        return """
            Actúa como un abogado experto en %s.
            Subespecialidad: %s.
            Descripción del caso: %s.
            Cuantía estimada: %s MXN.
            Jurisdicción: %s.
            Contraparte: %s.
            Estatus actual: %s.
            
            Genera un dictamen preliminar claro y estructurado,
            con pasos sugeridos y nivel de riesgo.
            """.formatted(
                entity.getCategory(),
                entity.getSubcategory(),
                entity.getDescription(),
                entity.getAmount(),
                entity.getLocation(),
                entity.getCounterparty(),
                entity.getProcessStatus()
        );
    }
}