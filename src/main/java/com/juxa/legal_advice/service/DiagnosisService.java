package com.juxa.legal_advice.service;

import com.juxa.legal_advice.config.JuxaPrices;
import com.juxa.legal_advice.dto.DiagnosisDTO;
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

    public void validarPrecio(double monto, String plan) {
        if (plan.equalsIgnoreCase("dictamen_detallado") && monto < JuxaPrices.SINGLE_DIAGNOSIS) {
            throw new RuntimeException("Pago insuficiente para Diagnóstico Único.");
        }
    }

    public DiagnosisResponse save(DiagnosisDTO dto) {
        DiagnosisEntity entity = new DiagnosisEntity();
        if (dto.getUserData() != null) {
            var userData = dto.getUserData();
            entity.setUserId(userData.getUserId());
            entity.setName(userData.getName());
            entity.setEmail(userData.getEmail());
            entity.setPhone(userData.getPhone());
            entity.setCategory(userData.getCategory());
            entity.setSubcategory(userData.getSubcategory());
            entity.setDescription(userData.getDescription());
            entity.setLocation(userData.getLocation());
            entity.setCounterparty(userData.getCounterparty());
            entity.setProcessStatus(userData.getProcessStatus());
            entity.setAmount(userData.getAmount());
            entity.setDiagnosisPreference(userData.getDiagnosisPreference());
        }
        entity.setFolio("FOL-" + System.currentTimeMillis());
        entity.setCreatedAt(LocalDateTime.now());
        DiagnosisEntity saved = diagnosisRepository.save(entity);
        return generateResponse(saved);
    }

    // ESTOS SON LOS MÉTODOS QUE EL CONTROLLER BUSCA:
    public DiagnosisDTO findById(String id) {
        return diagnosisRepository.findById(Long.parseLong(id))
                .map(this::mapToDTO)
                .orElse(null);
    }

    public List<DiagnosisDTO> findByUser(String userId) {
        return diagnosisRepository.findByUserId(userId)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    public List<DiagnosisDTO> findByUserEmail(String email) {
        return diagnosisRepository.findByEmail(email)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    public DiagnosisResponse generateResponse(DiagnosisEntity entity) {
        String summary = geminiService.generateLegalSummary(entity);
        var steps = Arrays.asList("Paso 1", "Paso 2");
        return DiagnosisResponse.builder()
                .diagnosisId(String.valueOf(entity.getId()))
                .summary(summary)
                .steps(steps)
                .build();
    }

    private DiagnosisDTO mapToDTO(DiagnosisEntity entity) {
        DiagnosisDTO dto = new DiagnosisDTO();
        dto.setId(String.valueOf(entity.getId()));
        dto.setFolio(entity.getFolio());
        return dto;
    }
    public DiagnosisResponse findResponseById(Long id) {
        return diagnosisRepository.findById(id)
                .map(this::generateResponse)
                .orElseThrow(() -> new RuntimeException("Diagnóstico no encontrado con ID: " + id));
    }
}