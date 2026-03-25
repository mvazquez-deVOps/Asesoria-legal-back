package com.juxa.legal_advice.service;

import com.juxa.legal_advice.config.JuxaPlanDef;
import com.juxa.legal_advice.dto.PlanResponseDTO;
import com.juxa.legal_advice.model.PlanEntity;
import com.juxa.legal_advice.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;

    public List<PlanResponseDTO> getAllPlans() {
        // Obtenemos todos los planes de la BD
        List<PlanEntity> dbPlans = planRepository.findAll();

        // Convertimos cada Entidad a nuestro DTO combinado
        return dbPlans.stream().map(plan -> {

            // Buscamos las características en el Enum usando el nombre de la BD
            JuxaPlanDef planDef = JuxaPlanDef.fromString(plan.getName());

            return PlanResponseDTO.builder()
                    // De la BD
                    .id(plan.getId())
                    .name(plan.getName())
                    .stripePriceId(plan.getStripePriceId())

                    // Del Enum actualizado
                    .maxTokens(planDef.getMaxTokens()) // <--- Ahora extraemos los tokens
                    .aiModel(planDef.getAiModel())
                    .canUploadAudio(planDef.isCanUploadAudio())
                    .canUploadVideo(planDef.isCanUploadVideo())
                    .hasFullHistory(planDef.isHasFullHistory())
                    .build();

        }).collect(Collectors.toList());
    }

    public String getStripePriceIdByPlanName(String planName) {
        PlanEntity plan = planRepository.findByName(planName)
                .orElseThrow(() -> new RuntimeException("Plan no encontrado en la BD: " + planName));
        return plan.getStripePriceId();
    }

    public PlanEntity getPlanByName(String planName) {
        return planRepository.findByName(planName)
                .orElseThrow(() -> new RuntimeException("Plan no encontrado en la BD: " + planName));
    }
}