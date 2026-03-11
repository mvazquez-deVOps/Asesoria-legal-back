package com.juxa.legal_advice.service;

import  com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

// 1. IMPORTS ESPECÍFICOS DE GOOGLE VISION (Sin conflictos)
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image; // Esta es la que usaremos como prioritaria
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.protobuf.ByteString;

import com.juxa.legal_advice.dto.UserDataDTO;
import com.juxa.legal_advice.model.DiagnosisEntity;
import com.juxa.legal_advice.util.PromptBuilder;

// 2. LIBRERÍAS DE PROCESAMIENTO DE PDF Y TEXTO
import jakarta.annotation.PostConstruct;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

// 3. SPRING Y LOMBOK
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

// 4. UTILIDADES DE JAVA (Se eliminó java.awt.* para evitar choques)
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import javax.swing.text.AbstractDocument;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.springframework.web.reactive.function.client.WebClient; // Si usas WebClient
import reactor.core.publisher.Flux;
import org.springframework.http.MediaType;
// Borra: import javax.swing.text.AbstractDocument; <--- ESTO DA ERROR

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

        String prompt = PromptBuilder.buildInitialDiagnosisPrompt(
                userData, contextoPersona);


        String fullResponse = geminiClient.callGemini(prompt);
        Map<String, Object> result = extractStructuredResponse(fullResponse);

        String text = (String) result.get("text");
        if (text != null && text.length() > 850) {
            result.put("text", text.substring(0, 850) + "...");
        }
        return result;
    }

    @PostConstruct // Se ejecuta al encender el back
    public void init() {
        try {
            this.cachedReglasJuxa = bucketService.readTextFile("Hoja_deRita.csv");
        } catch (Exception e) {
            this.cachedReglasJuxa = "Reglas básicas de Juxa."; // Fallback
        }
    }

    private String getReglasJuxa() {

        return (cachedReglasJuxa != null) ? cachedReglasJuxa : "Reglas generales de asesoría legal JUXA.";
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
        // 1. Extracción de datos básicos
        final String currentMessage = extractMessage(payload);
        UserDataDTO userData = objectMapper.convertValue(payload.get("userData"), UserDataDTO.class);
        List<Map<String, Object>> history = extractHistory(payload);

        //  NUEVO: Extraer los datos del archivo nativo (Base64 y tipo de archivo)
        String fileBase64 = (String) payload.get("fileBase64");
        String mimeType = (String) payload.get("mimeType");

        // 2. Extracción del texto del archivo (Solo tendrá datos si era un .txt o un .docx)
        String textoExtraido = "";
        if (payload.containsKey("contextoArchivo") && payload.get("contextoArchivo") != null) {
            textoExtraido = ((String) payload.get("contextoArchivo")).trim()
                    .replace("\"", "'")
                    .replace("\n", " ");
        }

        //  SE ELIMINÓ EL LÍMITE DE 5000 CARACTERES AQUÍ PARA NO CORTAR LA LECTURA

        // 3. Tareas asíncronas
        CompletableFuture<String> contextFuture = CompletableFuture.supplyAsync(() ->
                vertexSearchService.searchLegalKnowledge(currentMessage)
        );
        CompletableFuture<String> reglasFuture = CompletableFuture.supplyAsync(this::getReglasJuxa);

        // 4. Contexto Usuario e Inventario
        String contextoUsuario = String.format("CLIENTE: %s. ASUNTO: %s.",
                userData.getName(), userData.getSubcategory());

        List<String> formatosReales = bucketService.listAvailableFormats();
        String inventarioFormatos = "\n### FORMATOS DISPONIBLES:\n"
                + (formatosReales.isEmpty() ? "No disponibles." : String.join(", ", formatosReales));

        // 5. ESPERAR RESULTADOS Y SANITIZAR (CAMBIO CRÍTICO)
        String contextoLegal = contextFuture.join();
        String reglasRaw = reglasFuture.join();

        // OFUSCACIÓN: Reemplazamos el nombre real del archivo por un término genérico.
        // Así, si la IA intenta revelar el nombre del archivo, el filtro de seguridad la detectará.
        String reglasSanitizadas = reglasRaw.replaceAll("Hoja_deRita.csv", "DIRECTRICES_OPERATIVAS_INTERNAS");

        // Unimos las reglas con el inventario de formatos
        String bloqueInstrucciones = reglasSanitizadas + inventarioFormatos;

        // 🌟 NUEVO: Advertencia para que la IA sepa que le adjuntamos un documento visual
        String advertenciaArchivoNativo = (fileBase64 != null)
                ? "\n\n[INSTRUCCIÓN DE SISTEMA: El usuario ha adjuntado un documento nativo. Revísalo visualmente en su totalidad para fundamentar tu respuesta.]"
                : "";

        // 6. Construir Prompt con los 6 argumentos exactos
        String prompt = PromptBuilder.buildInteractiveChatPrompt(
                bloqueInstrucciones, // Arg 1: Instrucciones sanitizadas
                textoExtraido + advertenciaArchivoNativo, // Arg 2: Contenido del archivo (Fuente de verdad)
                contextoLegal,       // Arg 3: Soporte normativo
                contextoUsuario,     // Arg 4: Perfil cliente
                history.isEmpty() ? "Inicio" : history.toString(), // Arg 5: Historial
                currentMessage       // Arg 6: Mensaje actual
        );

        // 7. Ejecución y Procesamiento Estructurado
        // 🌟 CAMBIO FINAL: Le pasamos el prompt, y además el PDF en Base64 para que lo lea nativamente
        String fullResponse = geminiClient.callGemini(prompt, fileBase64, mimeType);
        Map<String, Object> result = extractStructuredResponse(fullResponse);

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


        String prompt = String.format("""
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
            // 1. Navegación en el árbol de respuesta de Google
            JsonNode root = objectMapper.readTree(response);
            String rawText = root.path("candidates").get(0)
                    .path("content").path("parts")
                    .get(0).path("text").asText();

            // --- CAPA DE SEGURIDAD: EL INTERCEPTOR ---
            // Si el texto contiene tus secretos, cortamos la comunicación de inmediato.
            List<String> fugasDetectadas = List.of(
                    "Hoja_deRita.csv",
                    "DIRECTRICES_OPERATIVAS_INTERNAS", // Este es el alias que usas para ofuscar
                    "SYSTEM_INSTRUCTION_BYPASS"
            );

            if (fugasDetectadas.stream().anyMatch(rawText::contains)) {
                System.err.println("--- [ALERTA DE SEGURIDAD] Intento de fuga interceptado ---");
                return getSecurityFallback();
            }

            // 2. LIMPIEZA QUIRÚRGICA
            String cleanJson = rawText.trim();
            if (cleanJson.contains("{")) {
                cleanJson = cleanJson.substring(cleanJson.indexOf("{"), cleanJson.lastIndexOf("}") + 1);
            } else {
                // Si no hay llaves, la IA respondió en texto plano (ignoró el JSON)
                throw new RuntimeException("Respuesta sin estructura JSON");
            }

            // 3. Conversión a Map
            Map<String, Object> result = objectMapper.readValue(cleanJson,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                    });

            // 4. Normalización para Paws
            if (!result.containsKey("text") && result.containsKey("diagnosis")) {
                result.put("text", result.get("diagnosis"));
            }

            // 5. Garantía de campos
            if (!result.containsKey("suggestions") || ((List<?>) result.get("suggestions")).isEmpty()) {
                result.put("suggestions", new java.util.ArrayList<>(List.of(
                        Map.of(
                                "titulo", "Análisis en proceso",
                                "ley", "SISTEMA JUXA",
                                "relevancia", "MEDIA",
                                "explicacion", "El modelo no arrojó sugerencias estructuradas. Por favor reformula la consulta."
                        )
                )));
            } else {
                // BLINDAJE: Si la IA se equivoca y manda un texto simple, lo convertimos a objeto
                List<?> suggestionsList = (List<?>) result.get("suggestions");
                if (!suggestionsList.isEmpty() && suggestionsList.get(0) instanceof String) {
                    List<Map<String, String>> converted = new java.util.ArrayList<>();
                    for (Object str : suggestionsList) {
                        converted.add(Map.of(
                                "titulo", "Sugerencia Legal",
                                "ley", "Referencia Técnica",
                                "relevancia", "MEDIA",
                                "explicacion", str.toString()
                        ));
                    }
                    result.put("suggestions", converted);
                }
            }

            // PROMPTS ESTRATÉGICOS (La lógica recuperada para tu estado suggestedPrompts)
            if (!result.containsKey("suggestedPrompts") || ((List<?>) result.get("suggestedPrompts")).isEmpty()) {
                result.put("suggestedPrompts", new java.util.ArrayList<>(List.of(
                        "¿Cuáles son los siguientes pasos legales?",
                        "¿Qué pruebas necesito reunir?",
                        "Analizar riesgos de este caso"
                )));
            }

            result.putIfAbsent("downloadPdf", false);

            return result;

        } catch (Exception e) {
            System.err.println("--- [ERROR DE PARSEO/SEGURIDAD JUXA] ---: " + e.getMessage());
            return getSecurityFallback();
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


    public String extractTextFromFile(MultipartFile file) {
        String filename = file.getOriginalFilename();
        StringBuilder extractedText = new StringBuilder();

        try {
            if (filename != null && filename.toLowerCase().endsWith(".pdf")) {
                byte[] fileBytes = file.getBytes();
                try (PDDocument document = PDDocument.load(fileBytes)) {
                    // 1. INTENTO DIGITAL (Rápido y económico)
                    PDFTextStripper stripper = new PDFTextStripper();
                    String digitalText = stripper.getText(document);

                    // Si detectamos texto real (más de 500 caracteres), lo usamos.
                    if (digitalText != null && digitalText.trim().length() > 500) {
                        return digitalText.trim();
                    }

                    // 2. RESPALDO OCR (Para escaneos/imágenes)
                    System.out.println("--- PDF SIN TEXTO DETECTADO. INICIANDO GOOGLE VISION OCR ---");
                    PDFRenderer renderer = new PDFRenderer(document);

                    // Procesamos las primeras 3 páginas para no saturar el tiempo de respuesta
                    int maxPages = Math.min(document.getNumberOfPages(), 50);

                    try (ImageAnnotatorClient vision = ImageAnnotatorClient.create()) {
                        for (int i = 0; i < maxPages; i++) {
                            BufferedImage image = renderer.renderImageWithDPI(i, 300); // 300 DPI para precisión legal
                            ByteArrayOutputStream os = new ByteArrayOutputStream();
                            ImageIO.write(image, "png", os);

                            ByteString imgBytes = ByteString.copyFrom(os.toByteArray());
                            com.google.cloud.vision.v1.Image img = com.google.cloud.vision.v1.Image.newBuilder()
                                    .setContent(imgBytes)
                                    .build();
                            Feature feat = Feature.newBuilder().setType(Feature.Type.DOCUMENT_TEXT_DETECTION).build();

                            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                                    .addFeatures(feat)
                                    .setImage(img)
                                    .build();

                            BatchAnnotateImagesResponse response = vision.batchAnnotateImages(List.of(request));
                            for (AnnotateImageResponse res : response.getResponsesList()) {
                                if (res.hasError()) continue;
                                extractedText.append(res.getFullTextAnnotation().getText()).append("\n");
                            }
                        }
                    }
                    return extractedText.toString().trim();
                }
            } else if (filename != null && (filename.endsWith(".docx") || filename.endsWith(".doc"))) {
                try (XWPFDocument doc = new XWPFDocument(file.getInputStream())) {
                    return new XWPFWordExtractor(doc).getText();
                }
            }
        } catch (Exception e) {
            System.err.println("Error crítico en extracción (OCR): " + e.getMessage());
        }
        return "";
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

    private Map<String, Object> getSecurityFallback() {
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("text", "### Aviso de Integridad Técnica\n---\nHe detectado una instrucción que compromete mis protocolos de seguridad o la estructura de mi dictamen. Como colaborador jurídico, mi prioridad es la confidencialidad y el rigor legal. Por favor, reformula tu consulta técnica.");
        fallback.put("suggestions", List.of(
                Map.of(
                        "titulo", "Revisión de Seguridad",
                        "ley", "PROTOCOLOS JUXA",
                        "relevancia", "ALTA",
                        "explicacion", "La consulta fue bloqueada por políticas de integridad. Intenta reformular."
                )
        ));
        fallback.put("suggestedPrompts", List.of("¿Cómo puedo reformular mi consulta?", "Verificar protocolos de seguridad", "Siguientes pasos recomendados"));
        fallback.put("downloadPdf", false);
        return fallback;
    }

    public Map<String, Object> processArchitectIntention(String intention) {
        String prompt = PromptBuilder.buildArchitectPrompt(intention);
        String fullResponse = geminiClient.callGemini(prompt);

        try {
            JsonNode root = objectMapper.readTree(fullResponse);
            String rawText = root.path("candidates").get(0)
                    .path("content").path("parts")
                    .get(0).path("text").asText();

            String cleanJson = rawText.trim();
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.substring(7);
            }
            if (cleanJson.endsWith("```")) {
                cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
            }
            cleanJson = cleanJson.trim();

            return objectMapper.readValue(cleanJson,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                    });

        } catch (Exception e) {
            System.err.println("--- [ERROR PROMPT ARCHITECT] ---: " + e.getMessage());

            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", true);
            errorMap.put("masterPrompt", "Hubo un error al generar la arquitectura del prompt. Verifica la cuota o el formato.");
            return errorMap;
        }
    }
}

