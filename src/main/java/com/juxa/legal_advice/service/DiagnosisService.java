package com.juxa.legal_advice.service;



import com.juxa.legal_advice.dto.DiagnosisRequestDTO;
import com.juxa.legal_advice.model.DiagnosisEntity;
import com.juxa.legal_advice.model.DiagnosisResponse;
import com.juxa.legal_advice.repository.DiagnosisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class DiagnosisService {

    private final DiagnosisRepository diagnosisRepository;
    private final GeminiService geminiService; // IA externa
    private final WhatsappService whatsappService; // Integración externa

    /**
     * Guarda el diagnóstico en la base de datos a partir del DTO recibido.
     */
    public DiagnosisEntity saveDiagnosis(DiagnosisRequestDTO dto) {
        DiagnosisEntity entity = DiagnosisEntity.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .category(dto.getCategory())
                .subcategory(dto.getSubcategory())
                .description(dto.getDescription())
                .amount(dto.getAmount())
                .location(dto.getLocation())
                .counterparty(dto.getCounterparty())
                .processStatus(dto.getProcessStatus())
                .build();

        return diagnosisRepository.save(entity);
    }

    /**
     * Genera la respuesta legal usando IA + lógica de negocio.
     */
    public DiagnosisResponse generateResponse(DiagnosisEntity entity) {
        // Llamada a Gemini para obtener un resumen legal
        String summary = geminiService.generateLegalSummary(entity);

        // Ejemplo de pasos sugeridos (pueden venir de Gemini o lógica propia)
        var steps = Arrays.asList(
                "Reunir evidencia documental",
                "Consultar abogado especializado en " + entity.getCategory(),
                "Evaluar cuantía estimada: $" + entity.getAmount(),
                "Definir estrategia procesal en " + entity.getLocation()
        );

        // Determinar nivel de riesgo según estatus
        String riskLevel = switch (entity.getProcessStatus()) {
            case "Preventivo" -> "Bajo";
            case "Notificado" -> "Moderado";
            case "En Juicio" -> "Alto";
            case "Emergencia" -> "Crítico";
            default -> "Indefinido";
        };

        // Enviar lead a WhatsApp (opcional)
        whatsappService.sendLead(entity);

        return DiagnosisResponse.builder()
                .diagnosisId(entity.getId())
                .summary(summary)
                .steps(steps)
                .riskLevel(riskLevel)
                .advisorNote("Este dictamen es preliminar, requiere validación humana.")
                .build();
    }

    /**
     * Recupera una respuesta previamente guardada.
     */
    public DiagnosisResponse findResponseById(Long id) {
        DiagnosisEntity entity = diagnosisRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Diagnóstico no encontrado"));

        return generateResponse(entity);
    }
}
