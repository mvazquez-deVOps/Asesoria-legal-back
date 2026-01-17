package com.juxa.legal_advice.service;

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

@Service
@RequiredArgsConstructor
public class GeminiService {

    private final GeminiClient geminiClient;
    private final AiBucketService bucketService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final VertexSearchService vertexSearchService;


    public Map<String, Object> generateInitialChatResponse(UserDataDTO userData) {
        String contextoPersona = "MORAL".equalsIgnoreCase(userData.getUserType()) ?
            contextoPersona = "el representante de la empresa" : "el ciudadano";

        String prompt = PromptBuilder.buildInitialDiagnosisPrompt(userData, contextoPersona);


        String fullResponse = geminiClient.callGemini(prompt);
        Map<String, Object> result = extractStructuredResponse(fullResponse);

        String text = (String) result.get("text");
        if (text != null && text.length() > 500) {
            result.put("text", text.substring(0, 500) + "...");
        }
        return result;
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
        // 1. Seguimos extrayendo los datos básicos
        String currentMessage = (String) payload.get("message");
        List<Map<String, Object>> history = (List<Map<String, Object>>) payload.get("history");
        UserDataDTO userData = objectMapper.convertValue(payload.get("userData"), UserDataDTO.class);

        // 2. REGLAS DE MISIÓN: Seguimos leyendo la Hoja_deRita manualmente
        // porque contiene las INSTRUCCIONES de comportamiento, no conocimiento genérico.
        String reglasJuxa = bucketService.readTextFile("Hoja_deRita.csv ");

        // 3. CONOCIMIENTO TÉCNICO
        // En una sola línea, obtenemos solo los fragmentos relevantes de toda la biblioteca.
        String contextoLegal = vertexSearchService.searchLegalKnowledge(currentMessage);

        System.out.println("DEBUG - Contexto recuperado de Vertex: " + contextoLegal);

        // 4. CONTEXTO DE USUARIO
        String contextoUsuario = String.format("CLIENTE: %s. ASUNTO: %s. PREFERENCIA: %s.",
                userData.getName(), userData.getSubcategory(), userData.getDiagnosisPreference());

        // 5. PROMPT MAESTRO: Enviamos a Gemini las reglas + los fragmentos exactos
        String prompt = PromptBuilder.buildInteractiveChatPrompt(
                reglasJuxa,
                contextoLegal,
                contextoUsuario,
                history != null ? history.toString() : "Inicio",
                currentMessage
        );

        String fullResponse = geminiClient.callGemini(prompt);
        Map<String, Object> result = extractStructuredResponse(fullResponse);

        // 6. UX: Mantenemos tu lógica de recordatorio cada 5 mensajes
        if (history != null) {
            long userQuestions = history.stream().filter(m -> "user".equals(m.get("role"))).count();
            if (userQuestions > 0 && userQuestions % 5 == 0) {
                result.put("reminder", "💡 JuxIA: ¿Consideras que ya me diste suficiente información?");
            }
        }

        return result;
    }

    public String generateLegalSummary(DiagnosisEntity entity) {
        String hechos = (entity.getDescription() != null) ? entity.getDescription() : "Caso por chat";
        String contexto = (entity.getHistory() != null) ? entity.getHistory() : "Sin historial";
         // Obtener las reglas y prompts asignados
        String reglasJuxa = bucketService.readTextFile("Hoja_deRita.csv");

        //Busqueda del agente


        String prompt = String.format( """
                Actúa como un abogado senior de JUXA. Genera un 'PLAN DE ACCIÓN JURÍDICA' profesional.
                HECHOS: %s. HISTORIAL: %s.
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

            //  LIMPIEZA: Eliminamos bloques de código markdown (```json ... ```)
            String cleanJson = rawText.replaceAll("(?s)```json\\s*|```", "").trim();

            if (cleanJson.startsWith("{")) {
                JsonNode parsed = objectMapper.readTree(cleanJson);
                Map<String, Object> result = new HashMap<>();

                // Extraemos los campos del JSON de la IA
                String diagnosis = parsed.path("diagnosis").asText();
                List<String> suggestions = new ArrayList<>();
                parsed.path("suggestions").forEach(node -> suggestions.add(node.asText()));
                boolean downloadPdf = parsed.path("downloadPdf").asBoolean();

                // Si el diagnosis viene vacío pero hay sugerencias, usamos la primera como texto
                if (diagnosis.isEmpty() && !suggestions.isEmpty()) {
                    diagnosis = suggestions.get(0);
                }

                result.put("text", diagnosis);
                result.put("suggestions", suggestions);
                result.put("downloadPdf", downloadPdf);
                return result;
            }

            // Si no es JSON, devolvemos el texto plano
            return Map.of("text", rawText, "suggestions", List.of(), "downloadPdf", false);
        } catch (Exception e) {
            return Map.of("text", "Error al procesar la respuesta legal.", "suggestions", List.of());
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
