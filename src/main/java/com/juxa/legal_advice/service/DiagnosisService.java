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

    public DiagnosisResponse save(DiagnosisDTO dto) {
        DiagnosisEntity entity = new DiagnosisEntity();
        if (dto.getUserData() != null) {
            var userData = dto.getUserData();
            entity.setUserId(userData.getUserId());
            // Nota: Si quitaste name/email de DiagnosisEntity para que solo estén en UserEntity,
            // comenta estas líneas. Si las dejaste como espejo, déjalas así:
            // entity.setName(userData.getName());
            // entity.setEmail(userData.getEmail());
            entity.setDescription(userData.getDescription());
        }

        if (dto.getHistory() != null) {
            String historyText = dto.getHistory().stream()
                    .map(m -> m.getRole() + ": " + m.getText())
                    .collect(Collectors.joining("\n"));
            entity.setHistory(historyText);
        }

        entity.setCreatedAt(LocalDateTime.now());
        entity.setPaid(true); // Usando el campo isPaid de la entidad

        return generateResponse(diagnosisRepository.save(entity));
    }

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

            if (entity.getId() == null && userDataMap != null) {
                entity.setUserId(userId);
                entity.setDescription((String) userDataMap.get("description"));
                entity.setCreatedAt(LocalDateTime.now());
                entity.setPaid(true);
            }

            String historialPrevio = (entity.getHistory() != null) ? entity.getHistory() : "";
            String nuevaInteraccion = "Usuario: " + userMsg + "\nIA: " + aiResponse + "\n\n";
            entity.setHistory(historialPrevio + nuevaInteraccion);

            diagnosisRepository.save(entity);

        } catch (Exception e) {
            System.err.println("Error en persistencia acumulativa v1.0.1: " + e.getMessage());
        }
    }

    public DiagnosisResponse generateResponse(DiagnosisEntity entity) {
        String summary = geminiService.generateLegalSummary(entity);
        return DiagnosisResponse.builder()
                .diagnosisId(String.valueOf(entity.getId()))
                .summary(summary)
                .steps(Arrays.asList("Consultar abogado experto", "Reunir evidencia"))
                .build();
    }

    public List<DiagnosisDTO> findByUser(String userId) {
        return diagnosisRepository.findByUserId(userId).stream()
                .map(this::mapToDTO).collect(Collectors.toList());
    }

    private DiagnosisDTO mapToDTO(DiagnosisEntity entity) {
        DiagnosisDTO dto = new DiagnosisDTO();
        dto.setId(String.valueOf(entity.getId()));
        // dto.setFolio(entity.getFolio()); // Comenta esta línea si eliminaste 'folio' de la entidad
        return dto;
    }

    public DiagnosisEntity findEntityById(Long id) {
        return diagnosisRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró el registro con ID: " + id));
    }

    public boolean canAccessPremiumFeatures(UserEntity user, DiagnosisEntity diagnosis) {
        if (user != null && "ADMIN".equals(user.getRole())) {
            return true;
        }
        return diagnosis != null && diagnosis.isPaid();
    }
}