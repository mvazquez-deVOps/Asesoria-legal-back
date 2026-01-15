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
    private final AiBucketService bucketService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> generateInitialChatResponse(UserDataDTO userData) {
        String contextoPersona = "el ciudadano";
        if ("MORAL".equalsIgnoreCase(userData.getUserType())) {
            contextoPersona = "el representante de la empresa";
        }

        String descripcion = userData.getDescription() != null ? userData.getDescription() : "";
        boolean contextoPobre = descripcion.length() <= 50;

        String prompt = String.format(
                "Eres abogado senior de JUXA, eres empático y quieres ayudar a resolver" +
                        "el caso. Analiza el caso de %s (%s): %s. " +
                        (contextoPobre
                                ? "Genera un diagnóstico inicial breve y 3 preguntas relevantes para guiar al cliente. "
                                : "Da un diagnóstico inicial breve y sugiere 3 preguntas específicas que el cliente podría hacerte a ti" +
                                "para que le des más información. ") +
                        "Responde únicamente en formato JSON válido con tres campos:\n" +
                        "{\n" +
                        "  \"diagnosis\": \"dictamen breve y técnico (máx 220 caracteres)\",\n" +
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
        if (text != null && text.length() > 220) {
            result.put("text", text.substring(0, 220) + "...");
        }

        return result;
    }
    public Map<String, Object> processInteractiveChat(Map<String, Object> payload) {
        String currentMessage = (String) payload.get("message");
        List<Map<String, Object>> history = (List<Map<String, Object>>) payload.get("history");

        // 1. OBTENER REGLAS DE ORO (Hoja de Ruta)
        String reglasJuxa = bucketService.readTextFile("CriticidadMaxima_HojadeRuta.xlsx - Hoja1.csv");

        // 2. BÚSQUEDA SEMÁNTICA EN BUCKET
        StringBuilder contextoDocumentos = new StringBuilder();
        List<String> docs = bucketService.listKnowledgeDocuments();
        for (String doc : docs) {
            if (currentMessage.toLowerCase().contains(doc.split("\\.")[0].toLowerCase())) {
                String content = doc.endsWith(".pdf") ?
                        bucketService.readPdfFile(doc) : bucketService.readTextFile(doc);
                contextoDocumentos.append("\n--- DOCUMENTO: ").append(doc).append(" ---\n").append(content);
            }
        }

        // 👤 CONTEXTO DEL USUARIO (Mantenemos tu lógica anterior)
        Map<String, Object> userData = (Map<String, Object>) payload.get("userData");
        String contextoUsuario = (userData != null) ?
                String.format("CLIENTE: %s. ASUNTO: %s.", userData.get("name"), userData.get("subcategory")) : "";

        // 3. PROMPT RAG POTENCIADO
        String prompt = String.format("""
                Eres JUXA Chat, experto legal.
                REGLAS DE MISIÓN (Hoja de Ruta): %s
                
                CONOCIMIENTO DEL BUCKET: %s
                
                %s. HISTORIAL: %s.
                MENSAJE ACTUAL: %s.
                
                RESPONDE SOLO JSON: {"diagnosis": "...", "suggestions": [...], "downloadPdf": false}
                """,
                reglasJuxa, contextoDocumentos, contextoUsuario,
                history != null ? history.toString() : "Inicio", currentMessage
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

    public String generateLegalSummary(DiagnosisEntity entity) {
        String hechos = (entity.getDescription() != null) ? entity.getDescription() : "Caso por chat";
        String contexto = (entity.getHistory() != null) ? entity.getHistory() : "Sin historial";
         // Obtener las reglas y prompts asignados
        String reglasJuxa = bucketService.readTextFile("Hoja_deRita.xlsx");

        //Busqueda del agente


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
