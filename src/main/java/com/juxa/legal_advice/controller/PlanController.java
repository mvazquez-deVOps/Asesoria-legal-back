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

import java.util.List;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;
    private final UserService userService;
    private final UsageAuthorizationService usageAuthService;

    @GetMapping
    public ResponseEntity<List<PlanResponseDTO>> getAvailablePlans() {
        List<PlanResponseDTO> plans = planService.getAllPlans();
        return ResponseEntity.ok(plans);
    }

   ////////////////////////// Mapea el plan de la base de datos de la tabla users ///////////////////////////
    @GetMapping("/me/usage")
    public ResponseEntity<UsageResponseDTO> getUserUsage() {
        UserEntity currentUser = userService.getCurrentAuthenticatedUser();
        JuxaPlanDef planDef = JuxaPlanDef.fromString(currentUser.getSubscriptionPlan());

        PlanUsageEntity usage = usageAuthService.getUsageStats(currentUser);

        // 1. Cálculos de Tokens
        int tokensUsados = usage.getTokensUsedToday() != null ? usage.getTokensUsedToday() : 0;
        int tokensExtra = usage.getExtraTokens() != null ? usage.getExtraTokens() : 0;
        int limiteTokens = planDef.getMaxTokens();

        int totalTokensDisponibles = limiteTokens + tokensExtra;

        // 2. Validación de estado (-1 significa ilimitado)
        boolean isUnlimited = (limiteTokens == -1);
        boolean canQuery = isUnlimited || (tokensUsados < totalTokensDisponibles);

        // 3. Construcción de la respuesta
        UsageResponseDTO response = UsageResponseDTO.builder()
                .planName(planDef.getDbName())

                // Nuevos datos de Tokens
                .tokensUsed(tokensUsados)
                .tokensLimit(limiteTokens)
                .extraTokens(tokensExtra)
                .canMakeMoreQueries(canQuery)
                .canUseProxy(planDef.isCanUseProxy())

                .canUseMiniApps(planDef.isCanUseMiniApps())
                .canUseGenerator(planDef.isCanUseGenerator())
                .canUseEducational(planDef.isCanUseEducational())
                .canUseAnalysis(planDef.isCanUseAnalysis())
                .canUseSustento(planDef.isCanUseSustento())
                .canUseSemantic(planDef.isCanUseSemantic())
                .canUseMagic(planDef.isCanUseMagic())

                // Datos estadísticos (ya no bloquean, solo informan)
                .queriesUsed(usage.getQueriesUsedToday() != null ? usage.getQueriesUsedToday() : 0)
                .filesUsed(usage.getFilesUploadedToday() != null ? usage.getFilesUploadedToday() : 0)

                // Capacidades del modelo
                .aiModel(planDef.getAiModel())
                .canUploadAudio(planDef.isCanUploadAudio())
                .canUploadVideo(planDef.isCanUploadVideo())
                .hasFullHistory(planDef.isHasFullHistory())
                .build();

        return ResponseEntity.ok(response);
    }
}