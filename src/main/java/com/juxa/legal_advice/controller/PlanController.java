package com.juxa.legal_advice.controller;

import com.juxa.legal_advice.config.JuxaPlanDef;
import com.juxa.legal_advice.dto.PlanResponseDTO;
import com.juxa.legal_advice.dto.UsageResponseDTO;
import com.juxa.legal_advice.model.PlanUsageEntity;
import com.juxa.legal_advice.model.UserEntity;
import com.juxa.legal_advice.service.PlanService;
import com.juxa.legal_advice.service.UsageAuthorizationService;
import com.juxa.legal_advice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;
    private final UserService userService;
    private final UsageAuthorizationService usageAuthService;


    // Puedes hacer que este endpoint sea público en tu SecurityConfig (.permitAll())
    // para que los usuarios no logueados puedan ver los precios.
    @GetMapping
    public ResponseEntity<List<PlanResponseDTO>> getAvailablePlans() {
        List<PlanResponseDTO> plans = planService.getAllPlans();
        return ResponseEntity.ok(plans);
    }

    @GetMapping("/me/usage")
    public ResponseEntity<UsageResponseDTO> getUserUsage() {
        UserEntity currentUser = userService.getCurrentAuthenticatedUser();
        JuxaPlanDef planDef = JuxaPlanDef.fromString(currentUser.getSubscriptionPlan());

        // Forzamos el reset por si entra al dashboard y es un nuevo día
        PlanUsageEntity usage = usageAuthService.getUsageStats(currentUser);

        // Construimos la respuesta fuertemente tipada
        UsageResponseDTO response = UsageResponseDTO.builder()
                .planName(planDef.getDbName())
                .queriesUsed(usage.getQueriesUsedToday())
                .queriesLimit(planDef.getMaxQueriesPerDay())
                .filesUsed(usage.getFilesUploadedToday())
                .filesLimit(planDef.getMaxFilesPerDay())
                .aiModel(planDef.getAiModel())
                .build();

        return ResponseEntity.ok(response);
    }
}