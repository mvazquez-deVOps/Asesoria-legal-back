package com.juxa.legal_advice.service;

import com.juxa.legal_advice.dto.DiagnosisDTO;
import com.juxa.legal_advice.model.DiagnosisEntity;
import com.juxa.legal_advice.model.DiagnosisResponse;
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
            entity.setName(userData.getName());
            entity.setEmail(userData.getEmail());
            entity.setCategory(userData.getCategory());
            entity.setSubcategory(userData.getSubcategory());
            entity.setDescription(userData.getDescription());
            entity.setLocation(userData.getLocation());
        }

        if (dto.getHistory() != null) {
            String historyText = dto.getHistory().stream()
                    .map(m -> m.getRole() + ": " + m.getText())
                    .collect(Collectors.joining("\n"));
            entity.setHistory(historyText);
        }

        entity.setFolio("FOL-" + System.currentTimeMillis());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setPaid(true);

        return generateResponse(diagnosisRepository.save(entity));
    }

    public void saveFromChat(Map<String, Object> payload, String aiResponse) {
        try {
            Map<String, Object> userDataMap = (Map<String, Object>) payload.get("userData");
            String userMsg = (String) payload.get("message");

            DiagnosisEntity entity = new DiagnosisEntity();
            if (userDataMap != null) {
                entity.setName((String) userDataMap.get("name"));
                entity.setEmail((String) userDataMap.get("email"));
                entity.setCategory((String) userDataMap.get("category"));
                entity.setSubcategory((String) userDataMap.get("subcategory"));
                entity.setDescription((String) userDataMap.get("description"));
            }

            entity.setHistory("Usuario: " + userMsg + "\nIA: " + aiResponse);
            entity.setFolio("FOL-CHAT-" + System.currentTimeMillis());
            entity.setCreatedAt(LocalDateTime.now());
            entity.setPaid(true);

            diagnosisRepository.save(entity);
        } catch (Exception e) {
            System.err.println("Error en saveFromChat: " + e.getMessage());
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
        dto.setFolio(entity.getFolio());
        return dto;
    }
    public DiagnosisDTO findById(String id) {
        return diagnosisRepository.findById(Long.parseLong(id))
                .map(this::mapToDTO)
                .orElse(null);
    }

    public List<DiagnosisDTO> findByUserEmail(String email) {
        return diagnosisRepository.findByEmail(email).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    // Agrega este método para que el Controller deje de marcar error
    public DiagnosisEntity findEntityById(Long id) {
        return diagnosisRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró el registro con ID: " + id));
    }
}