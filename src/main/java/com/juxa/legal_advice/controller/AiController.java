package com.juxa.legal_advice.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.juxa.legal_advice.config.JuxaPlanDef;
import com.juxa.legal_advice.config.exceptions.AppNotAllowedForSubscriptionException;
import com.juxa.legal_advice.config.exceptions.PlanLimitExceededException;
import com.juxa.legal_advice.config.exceptions.TokenConfirmationRequiredException;
import com.juxa.legal_advice.dto.ProxyGenerateRequest;
import com.juxa.legal_advice.dto.UserDataDTO;
import com.juxa.legal_advice.model.UserEntity;
import com.juxa.legal_advice.repository.UserRepository;
import com.juxa.legal_advice.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;///////////////////////////////////////////////////////////
import org.slf4j.LoggerFactory;/////////////////////////////////////

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AiController {

    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final AiBucketService aiBucketService;
    private final GeminiClient geminiClient;

    @Autowired
    private  UsageAuthorizationService usageAuthService;
    @Autowired
    private  UserService userService;
    private static final Logger log = LoggerFactory.getLogger(AiController.class); ////////////////


    @Autowired(required = false)
    private Storage storage;

    @PostMapping("/generate-initial-diagnosis")
    public ResponseEntity<Map<String, Object>> startDiagnosis(@RequestBody UserDataDTO userData) {
      //  UserEntity currentUser = userService.getCurrentAuthenticatedUser();
       // usageAuthService.authorizeAndConsumeQuery(currentUser);

        Map<String, Object> response = geminiService.generateInitialChatResponse(userData);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/chat", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> chat(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Confirm-Token", defaultValue = "false") boolean isConfirmed,
            @RequestParam("message") String currentMessage,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam("userData") String userDataJson,
            @RequestParam("history") String historyJson) {
        try {
            UserEntity currentUser = userService.getCurrentAuthenticatedUser();

            // 1. Definición del nombre de la herramienta para el Peaje
            String toolName = "CHAT";

            // 2. Procesamiento de Datos del Usuario e Historial
            Map<String, Object> userDataMap = objectMapper.readValue(userDataJson, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> historyList = objectMapper.readValue(historyJson, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});

            // 3. Lógica de Archivos y OCR (Tu lógica original completa)
            String fileBase64 = null;
            String mimeType = null;
            String textoOcr = "";

            if (file != null && !file.isEmpty()) {
                String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
                if (filename.endsWith(".pdf") || filename.endsWith(".png") || filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
                    fileBase64 = java.util.Base64.getEncoder().encodeToString(file.getBytes());
                    mimeType = file.getContentType();
                    if (mimeType == null || mimeType.contains("octet-stream")) {
                        mimeType = filename.endsWith(".pdf") ? "application/pdf" : "image/jpeg";
                    }
                    textoOcr = "A".repeat(12000); // Buffer para análisis visual
                } else {
                    textoOcr = geminiService.extractTextFromFile(file);
                }
            }

            // 4. Estimación de Tokens y Peaje de Entrada
            int tokensInput = (currentMessage.length() + textoOcr.length() + historyJson.length()) / 4;
            int estimate = tokensInput + 1500;
            usageAuthService.validateSufficientTokens(currentUser, "CHAT", estimate, isConfirmed, toolName);

            // 5. Preparación de Contexto (Bucket y Hoja de Ruta)
            String directrices = aiBucketService.readTextFile("Hoja_deRita.csv");
            String contextoCarpetas = generarContextoBucket();

            Map<String, Object> payload = new HashMap<>();
            payload.put("message", currentMessage);
            payload.put("userData", userDataMap);
            payload.put("history", historyList);
            payload.put("hojaRuta", directrices);
            payload.put("estructuraCarpetas", contextoCarpetas);
            payload.put("textoOcr", textoOcr.startsWith("A".repeat(100)) ? "" : textoOcr);
            payload.put("fileBase64", fileBase64);
            payload.put("mimeType", mimeType);

            // 6. Llamada a la IA
            Map<String, Object> aiResponse = geminiService.processInteractiveChat(payload);

            // 7. Deducción Real de Tokens
            if (aiResponse.containsKey("_usageMetadata")) {
                Map<String, Object> metadata = (Map<String, Object>) aiResponse.get("_usageMetadata");
                Number totalSpent = (Number) metadata.getOrDefault("totalTokens", 0);
                usageAuthService.consumeTokens(currentUser, "CHAT", totalSpent.intValue(), toolName);
            }

            return ResponseEntity.ok(aiResponse);

        } catch (PlanLimitExceededException | TokenConfirmationRequiredException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error en Chat: ", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    @PostMapping("/architect")
    public ResponseEntity<Map<String, Object>> generateArchitectPrompt(@RequestBody Map<String, String> payload) {
        try {
            String intention = payload.get("intention");
            Map<String, Object> response = geminiService.processArchitectIntention(intention);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    @PostMapping("/proxy-generate")
    public ResponseEntity<Map<String, Object>> generateProxyPrompt(
            @RequestHeader(value = "X-Confirm-Token", defaultValue = "false") boolean isConfirmed,
            @RequestBody ProxyGenerateRequest payload) {
        try {
            // --- 🔍 LOG DE DEBUG PARA MARIANA ---
            System.out.println(">>> RECIBIENDO PETICIÓN EN PROXY-GENERATE");
            System.out.println(">>> Prompt: " + (payload.getPrompt() != null ? "Recibido" : "NULO"));
            System.out.println(">>> ToolName recibido del Front: [" + payload.getToolName() + "]");
            // ------------------------------------

            UserEntity currentUser = userService.getCurrentAuthenticatedUser();
            JuxaPlanDef planDef = JuxaPlanDef.fromString(currentUser.getSubscriptionPlan());

            // Aseguramos que no se convierta en "general" si viene algo
            String toolName = payload.getToolName();
            if (toolName == null || toolName.trim().isEmpty()) {
                toolName = "general"; // Solo aquí se vuelve "general"
            }
            System.out.println("DEBUG: Procesando herramienta -> " + toolName);
            String prompt = payload.getPrompt();


            // 2. Validar si el plan permite la herramienta específica
            if (!planDef.isToolAllowed(toolName)) {
                throw new AppNotAllowedForSubscriptionException(
                        "Tu plan " + planDef.getDbName() + " no permite usar la herramienta: " + toolName
                );
            }

            // 3. DETERMINAR EL MÓDULO (Para saber qué bolsa de tokens tocar)
            String module = "APPS"; // Por defecto

            // Lógica para detectar el módulo según el toolName
            if (toolName.toLowerCase().contains("doc") || toolName.toLowerCase().contains("editor")) {
                module = "DOCS";
            } else if (isMiniApp(toolName)) {
                module = "MINI_APPS";
            }

            // Estimación de tokens (chars / 4 + buffer)
            int estimate = (prompt.length() / 4) + 1500;

            // 4. PEAJE DE ENTRADA (Ahora enviamos el toolName para aplicar las excepciones)
            // Pasamos: (Usuario, Módulo, Estimación, Confirmación, Herramienta)
            usageAuthService.validateSufficientTokens(currentUser, module, estimate, isConfirmed, toolName);

            // 5. EJECUCIÓN EN GEMINI
            String aiResponse = geminiClient.callGemini(prompt);

            // 6. PROCESAMIENTO DE RESPUESTA
            JsonNode rootNode = objectMapper.readTree(aiResponse);
            String cleanText = rootNode.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();

            // 7. CONSUMO REAL (Deducción post-respuesta)
            JsonNode usageNode = rootNode.path("usageMetadata");
            if (!usageNode.isMissingNode()) {
                int totalSpent = usageNode.path("totalTokenCount").asInt(0);

                // Pasamos el toolName también aquí para que el Service decida si cobrar o no
                usageAuthService.consumeTokens(currentUser, module, totalSpent, toolName);
            }

            return ResponseEntity.ok(Map.of("rawResponse", cleanText));

        } catch (AppNotAllowedForSubscriptionException | PlanLimitExceededException | TokenConfirmationRequiredException e) {
            // Estas excepciones las captura el GlobalExceptionHandler para el Front
            throw e;
        } catch (Exception e) {
            log.error("Error crítico en Proxy Generate: ", e);
            return ResponseEntity.status(500).body(Map.of("error", "Error interno en el procesamiento legal."));
        }
    }

    /**
     * Método auxiliar para identificar si la herramienta es una Mini App
     */
    private boolean isMiniApp(String toolName) {
        List<String> miniApps = List.of(
                "redactor-hechos", "exam", "guide", "tipicidad",
                "evidence-validator", "medidas-cautelares", "generador-interrogatorios"
        );
        return miniApps.contains(toolName.toLowerCase());
    }


    private String generarContextoBucket() {
        Map<String, String> carpetaContexto = Map.of(
                "Camara_de_Diputados/", "Acuerdos legislativos.",
                "FORMATOS/", "Plantillas procesales.",
                "Mercantil/", "Normativa mercantil.",
                "Marco-Recomendable/", "Guías doctrinales.",
                "Imprescindibles/", "Documentos críticos.",
                "Códigos_Civiles_Penales_Procedimientos/", "Códigos estatales."
        );

        StringBuilder contexto = new StringBuilder("### PROTOCOLO DE CONSULTA INTERNA (BUCKET)\n");
        carpetaContexto.forEach((carpeta, desc) -> contexto.append("- ").append(carpeta).append(": ").append(desc).append("\n"));

        try {
            Page<Blob> blobs = storage.list("asesoria-legal-bucket", Storage.BlobListOption.prefix("Códigos_Civiles_Penales_Procedimientos/"), Storage.BlobListOption.currentDirectory());
            contexto.append("   Subcarpetas estatales:\n");
            for (Blob blob : blobs.iterateAll()) {
                if (blob.isDirectory()) {
                    String nombreEstado = blob.getName().replace("Códigos_Civiles_Penales_Procedimientos/", "").replace("/", "").replace("_", " ");
                    contexto.append("   - ").append(nombreEstado).append("\n");
                }
            }
        } catch (Exception e) {}
        return contexto.toString();
    }

    //Metodo para abrir pdfs en JUXA docs
    @PostMapping(value = "/extract-text", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> extractTextFromDocument(@RequestParam("file") MultipartFile file) {
        try {
            // Misma lógica de GeminiService para leer PDFs o .doc
            String textoExtraido = geminiService.extractTextFromFile(file);
            String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";

            // Verificamos si es un .doc para indicarle al frontend que es HTML
            boolean isHtml = filename.endsWith(".doc");

            // Devolvemos tanto el texto como la bandera
            return ResponseEntity.ok(Map.of(
                    "extractedText", textoExtraido,
                    "isHtml", isHtml
            ));
        } catch (Exception e) {
            e.printStackTrace(); // Ver el error en consola de Java
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}