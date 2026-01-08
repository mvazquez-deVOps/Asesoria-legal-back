package com.juxa.legal_advice.service;

import com.juxa.legal_advice.dto.DiagnosisDTO;
import com.juxa.legal_advice.model.DiagnosisEntity;
import com.juxa.legal_advice.model.DiagnosisResponse;
import com.juxa.legal_advice.model.UserEntity;
import com.juxa.legal_advice.repository.DiagnosisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiagnosisService {

    private final DiagnosisRepository diagnosisRepository;
    private final GeminiService geminiService;

    // 1. Guardar diagnóstico normal
    public DiagnosisResponse save(DiagnosisDTO dto) {
        DiagnosisEntity entity = new DiagnosisEntity();
        if (dto.getUserData() != null) {
            entity.setUserId(dto.getUserData().getUserId());
            entity.setDescription(dto.getUserData().getDescription());
        }
        entity.setCreatedAt(LocalDateTime.now());
        entity.setPaid(true);
        return generateResponse(diagnosisRepository.save(entity));
    }

    // 2. Guardar interacción del chat (EL QUE TE FALTA)
    public void saveFromChat(Map<String, Object> payload, String aiResponse) {
        try {
            Map<String, Object> userDataMap = (Map<String, Object>) payload.get("userData");
            String userMsg = (String) payload.get("message");
            String userId = (userDataMap != null) ? (String) userDataMap.get("userId") : null;

            DiagnosisEntity entity;
            if (userId != null) {
                entity = diagnosisRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                        .orElse(new DiagnosisEntity());
            } else {
                entity = new DiagnosisEntity();
            }

            if (entity.getId() == null) {
                entity.setUserId(userId);
                entity.setCreatedAt(LocalDateTime.now());
                entity.setPaid(true);
            }

            String historialPrevio = (entity.getHistory() != null) ? entity.getHistory() : "";
            entity.setHistory(historialPrevio + "Usuario: " + userMsg + "\nIA: " + aiResponse + "\n\n");

            diagnosisRepository.save(entity);
        } catch (Exception e) {
            System.err.println("Error guardando chat: " + e.getMessage());
        }
    }

    // 3. Buscar por ID para el controlador
    public DiagnosisDTO findById(String id) {
        return diagnosisRepository.findById(Long.parseLong(id))
                .map(this::mapToDTO)
                .orElseThrow(() -> new RuntimeException("No encontrado"));
    }

    // 4. Buscar por Usuario para el controlador
    public List<DiagnosisDTO> findByUser(String userId) {
        return diagnosisRepository.findByUserId(userId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private DiagnosisDTO mapToDTO(DiagnosisEntity entity) {
        DiagnosisDTO dto = new DiagnosisDTO();
        dto.setId(String.valueOf(entity.getId()));
        return dto;
    }

    public DiagnosisResponse generateResponse(DiagnosisEntity entity) {
        String summary = geminiService.generateLegalSummary(entity);
        return DiagnosisResponse.builder()
                .diagnosisId(String.valueOf(entity.getId()))
                .summary(summary)
                .steps(Arrays.asList("Consultar abogado experto", "Reunir evidencia"))
                .build();
    }

    public DiagnosisEntity findEntityById(Long id) {
        return diagnosisRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró el registro con ID: " + id));
    }
}