package com.juxa.legal_advice.service;

import com.juxa.legal_advice.dto.DiagnosisDTO;
import com.juxa.legal_advice.dto.DiagnosisRequestDTO;
import com.juxa.legal_advice.model.DiagnosisEntity;
import com.juxa.legal_advice.model.DiagnosisResponse;
import com.juxa.legal_advice.repository.DiagnosisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DiagnosisService {

    private final DiagnosisRepository diagnosisRepository;
    private final GeminiService geminiService;
    private final WhatsappService whatsappService;

    /** Guardar diagnóstico a partir de DTO */
    public DiagnosisDTO save(DiagnosisDTO dto) {
        DiagnosisEntity entity = DiagnosisEntity.builder()
                .userId(dto.getUserData().getUserId()) // asegúrate de tener este campo en UserDataDTO y DiagnosisEntity
                .name(dto.getUserData().getName())
                .email(dto.getUserData().getEmail())
                .phone(dto.getUserData().getPhone())
                .category(dto.getUserData().getCategory())
                .subcategory(dto.getUserData().getSubcategory())
                .description(dto.getUserData().getDescription())
                .amount(Double.parseDouble(dto.getUserData().getAmount()))
                .location(dto.getUserData().getLocation())
                .counterparty(dto.getUserData().getCounterparty())
                .processStatus(dto.getUserData().getProcessStatus())
                .folio(generateFolio())
                .createdAt(LocalDateTime.now())
                .build();

        DiagnosisEntity saved = diagnosisRepository.save(entity);
        return mapToDTO(saved);
    }

    /** Buscar por ID */
    public DiagnosisDTO findById(String id) {
        return diagnosisRepository.findById(Long.parseLong(id))
                .map(this::mapToDTO)
                .orElse(null);
    }

    /** Buscar por email */
    public List<DiagnosisDTO> findByUserEmail(String email) {
        return diagnosisRepository.findByEmail(email)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    /** Buscar por userId */
    public List<DiagnosisDTO> findByUser(String userId) {
        return diagnosisRepository.findByUserId(userId)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    /** Generar respuesta legal */
    public DiagnosisResponse generateResponse(DiagnosisEntity entity) {
        String summary = geminiService.generateLegalSummary(entity);

        var steps = Arrays.asList(
                "Reunir evidencia documental",
                "Consultar abogado especializado en " + entity.getCategory(),
                "Evaluar cuantía estimada: $" + entity.getAmount(),
                "Definir estrategia procesal en " + entity.getLocation()
        );

        String riskLevel = switch (entity.getProcessStatus()) {
            case "Preventivo" -> "Bajo";
            case "Notificado" -> "Moderado";
            case "En Juicio" -> "Alto";
            case "Emergencia" -> "Crítico";
            default -> "Indefinido";
        };

        whatsappService.sendLead(entity);

        return DiagnosisResponse.builder()
                .diagnosisId(String.valueOf(entity.getId()))
                .summary(summary)
                .steps(steps)
                .riskLevel(riskLevel)
                .advisorNote("Este dictamen es preliminar, requiere validación humana.")
                .build();
    }

    /** Mapear entidad a DTO */
    private DiagnosisDTO mapToDTO(DiagnosisEntity entity) {
        DiagnosisDTO dto = new DiagnosisDTO();
        dto.setId(String.valueOf(entity.getId()));
        dto.setStatus(entity.getProcessStatus());
        dto.setFolio(entity.getFolio());
        dto.setCreatedAt(entity.getCreatedAt().toString());
        // dto.setUserData(...) si decides mapearlo también
        return dto;
    }

    /** Generar folio único */
    private String generateFolio() {
        return "FOL-" + System.currentTimeMillis();
    }
    public DiagnosisResponse findResponseById(Long id) {
        return diagnosisRepository.findById(id)
                .map(this::generateResponse)
                .orElse(null);
    }
}