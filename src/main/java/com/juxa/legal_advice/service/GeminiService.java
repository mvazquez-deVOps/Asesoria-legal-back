package com.juxa.legal_advice.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.juxa.legal_advice.dto.UserDataDTO;
import com.juxa.legal_advice.model.DiagnosisEntity;
import com.juxa.legal_advice.util.PromptBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.supplyAsync;

@Service
@RequiredArgsConstructor
public class GeminiService {

    private final GeminiClient geminiClient;
    private final AiBucketService bucketService;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final VertexSearchService vertexSearchService;


    public String callGemini(String prompt) {
        return geminiClient.callGemini(prompt);
    }

    private String cachedReglasJuxa = null;




    public Map<String, Object> generateInitialChatResponse(UserDataDTO userData) {
        String contextoPersona = "MORAL".equalsIgnoreCase(userData.getUserType()) ?
             "el representante de la empresa" : "el ciudadano";

        String prompt = PromptBuilder.buildInitialDiagnosisPrompt(userData, contextoPersona);


        String fullResponse = geminiClient.callGemini(prompt);
        Map<String, Object> result = extractStructuredResponse(fullResponse);

        String text = (String) result.get("text");
        if (text != null && text.length() > 500) {
            result.put("text", text.substring(0, 500) + "...");
        }
        return result;
    }

    private String getReglasJuxa() {
        if (cachedReglasJuxa == null) {
            cachedReglasJuxa = bucketService.readTextFile("Hoja_deRita.csv");
        }
        return cachedReglasJuxa;
    }

    /* Esta versión se utilizaba cuando se requería buscar archivo por archivo
    public Map<String, Object> processInteractiveChat(Map<String, Object> payload) {
        String currentMessage = (String) payload.get("message");
        List<Map<String, Object>> history = (List<Map<String, Object>>) payload.get("history");
        Map<String, Object> userDataMap = (Map<String, Object>) payload.get("userData");

        String reglasJuxa = bucketService.readTextFile("Hoja_deRita.csv");

        StringBuilder contextoDocumentos = new StringBuilder();
        List<String> docs = bucketService.listKnowledgeDocuments();
        for (String doc : docs) {
            if (currentMessage.toLowerCase().contains(doc.split("\\.")[0].toLowerCase())) {
                String content = doc.endsWith(".pdf") ?
                        bucketService.readPdfFile(doc) : bucketService.readTextFile(doc);
                contextoDocumentos.append("\n--- DOCUMENTO: ").append(doc).append(" ---\n").append(content);
            }
        }

        String contextoUsuario = (userDataMap != null) ?
                String.format("CLIENTE: %s. ASUNTO: %s.", userDataMap.get("name"), userDataMap.get("subcategory")) : "";

        // 3. PROMPT RAG POTENCIADO
        String prompt = PromptBuilder.buildInteractiveChatPrompt(
                reglasJuxa,
                contextoDocumentos.toString(),
                contextoUsuario,
                history != null ? history.toString() : "Inicio",
                currentMessage
        );

        String fullResponse = geminiClient.callGemini(prompt);
        Map<String, Object> result = extractStructuredResponse(fullResponse);

        // Mantenemos tu recordatorio cada 5 mensajes
        if (history != null) {
            long userQuestions = history.stream().filter(m -> "user".equals(m.get("role"))).count();
            if (userQuestions > 0 && userQuestions % 5 == 0) {
                result.put("reminder", "💡 JuxIA: ¿Consideras que ya me diste suficiente información?");
            }
        }

        return result;
    }
*/
    public Map<String, Object> processInteractiveChat(Map<String, Object> payload) {
        // --- PASO 1: Extracción y Limpieza (Operaciones CPU rápidas) ---
       final String currentMessage = extractMessage(payload); // Usa tu helper o el logic de abajo


        UserDataDTO userData = objectMapper.convertValue(payload.get("userData"), UserDataDTO.class);
        List<Map<String, Object>> history = extractHistory(payload);

        // Disparar Tareas Asíncronas
        // Lanzamos la búsqueda en Vertex y la obtención de reglas al mismo tiempo
        CompletableFuture<String> contextFuture = CompletableFuture.supplyAsync(() ->
                vertexSearchService.searchLegalKnowledge(currentMessage)
        );

        CompletableFuture<String> reglasFuture = CompletableFuture.supplyAsync(this::getReglasJuxa);

        // --- PASO 3: Preparar Contexto de Usuario (Mientras Vertex trabaja) ---
        String contextoUsuario = String.format("CLIENTE: %s. ASUNTO: %s. PREFERENCIA: %s.",
                userData.getName() != null ? userData.getName() : "Desconocido",
                userData.getSubcategory() != null ? userData.getSubcategory() : "Sin categoría",
                userData.getDiagnosisPreference() != null ? userData.getDiagnosisPreference() : "Sin preferencia");

        // --- PASO 4: Sincronización (Join) ---
        // Aquí el código espera solo si Vertex aún no termina.
        // Si Vertex tardó 3s y procesar el resto tardó 1s, solo "esperas" 2s.
        String contextoLegal = contextFuture.join();
        String reglasJuxa = reglasFuture.join();

        // --- PASO 5: Prompt y Llamada a Gemini ---
        String prompt = PromptBuilder.buildInteractiveChatPrompt(
                reglasJuxa,
                contextoLegal,
                contextoUsuario,
                history.isEmpty() ? "Inicio" : history.toString(),
                currentMessage);

        // Llamada final al modelo
        String fullResponse = geminiClient.callGemini(prompt);

        // --- PASO 6: Estructuración y UX ---
        Map<String, Object> result = extractStructuredResponse(fullResponse);

        // Inyectar lógica de recordatorio
        injectReminderLogic(result, history);

        return result;
    }

    // Helper para no ensuciar el método principal
    private void injectReminderLogic(Map<String, Object> result, List<Map<String, Object>> history) {
        long userQuestions = history.stream()
                .filter(m -> "user".equals(m.get("role")))
                .count();
        if (userQuestions > 0 && userQuestions % 5 == 0) {
            result.put("reminder", "💡 JuxIA: ¿Consideras que ya me diste suficiente información?");
        }
    }
    public String generateLegalSummary(DiagnosisEntity entity) {
        String hechos = (entity.getDescription() != null) ? entity.getDescription() : "Caso por chat";
        String contexto = (entity.getHistory() != null) ? entity.getHistory() : "Sin historial";
         // Obtener las reglas y prompts asignados
        String reglasJuxa = getReglasJuxa();

        //Busqueda del agente


        String prompt = String.format( """
                Actúa como un abogado senior de JUXA. Genera un 'PLAN DE ACCIÓN JURÍDICA' profesional.
                REGLAS DE OPERACION: %s.
                HECHOS: %s. HISTORIAL: %s.
                Genera un plan de acción jurídica profesional.
                Divide en: 1. RESUMEN, 2. FUNDAMENTACIÓN, 3. ACCIONES, 4. PROCEDIMIENTO, 5. RECOMENDACIÓN.
                """, reglasJuxa, hechos, contexto);

        String fullResponse = geminiClient.callGemini(prompt);
        return extractTextFromResponse(fullResponse);
    }

    // Nuevo extractor estructurado: diagnosis + suggestions
    private Map<String, Object> extractStructuredResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            String rawText = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();

            int start = rawText.indexOf("{");
            int end = rawText.lastIndexOf("}");

            if (start != -1 && end != -1) {
                String cleanJson = rawText.substring(start, end + 1);
                JsonNode parsed = objectMapper.readTree(cleanJson);
                Map<String, Object> result = new HashMap<>();

                // SINCRONIZACIÓN: Leemos "text" porque es lo que pide el PromptBuilder
                String responseText = parsed.has("text") ? parsed.path("text").asText() : parsed.path("diagnosis").asText();
                result.put("text", responseText);

                List<String> suggestions = new ArrayList<>();
                if (parsed.has("suggestions")) {
                    parsed.path("suggestions").forEach(node -> suggestions.add(node.asText()));
                }

                result.put("text", responseText);
                result.put("suggestions", suggestions);
                result.put("downloadPdf", parsed.path("downloadPdf").asBoolean());
                return result;
            }
            return Map.of("text", rawText, "suggestions", List.of(), "downloadPdf", false);
        } catch (Exception e) {
            return Map.of("text", "Error de formato en la respuesta legal.", "suggestions", List.of());
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
    /**
     * Extrae el mensaje actual del payload de forma segura,
     * verificando múltiples llaves posibles.
     */
    private String extractMessage(Map<String, Object> payload) {
        String message = (String) payload.get("currentMessage");
        if (message == null) {
            message = (String) payload.get("message");
        }
        return (message != null && !message.trim().isEmpty())
           ? message.trim()
           : "Continuar con el análisis legal basado en los hechos anteriores.";
    }

    /**
     * Extrae y castea la lista del historial de chat de forma segura
     * para evitar ClassCastException.
     */
    private List<Map<String, Object>> extractHistory(Map<String, Object> payload) {
        List<Map<String, Object>> history = new ArrayList<>();
        Object rawHistory = payload.get("history");

        if (rawHistory instanceof List<?>) {
            for (Object item : (List<?>) rawHistory) {
                if (item instanceof Map<?, ?>) {
                    // Casteo seguro de cada entrada del historial
                    history.add((Map<String, Object>) item);
                }
            }
        }
        return history;
    }
}
