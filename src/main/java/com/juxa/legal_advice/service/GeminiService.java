package com.juxa.legal_advice.service;



import com.juxa.legal_advice.model.DiagnosisEntity;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GeminiService {

    private final GeminiClient geminiClient; // Cliente HTTP configurado en GeminiConfig

    /**
     * Genera un resumen legal usando Gemini a partir de la entidad Diagnosis.
     */
    public String generateLegalSummary(DiagnosisEntity entity) {
        // Construcción del prompt legal
        String prompt = buildPrompt(entity);

        // Llamada al cliente Gemini (ej. REST API)
        String response = geminiClient.callGemini(prompt);

        // Aquí puedes aplicar post-procesamiento (ej. limpiar texto, truncar, enriquecer)
        return response;
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
