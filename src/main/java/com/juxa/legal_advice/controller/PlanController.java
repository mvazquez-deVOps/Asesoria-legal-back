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
public class  PlanController {

    private final PlanService planService;
    private final UserService userService;
    private final UsageAuthorizationService usageAuthService;

    @GetMapping
    public ResponseEntity<List<PlanResponseDTO>> getAvailablePlans() {
        List<PlanResponseDTO> plans = planService.getAllPlans();
        return ResponseEntity.ok(plans);
    }

    @GetMapping("/me/usage")
    public ResponseEntity<UsageResponseDTO> getUserUsage() {
        UserEntity currentUser = userService.getCurrentAuthenticatedUser();
        JuxaPlanDef planDef = JuxaPlanDef.fromString(currentUser.getSubscriptionPlan());

        // Obtenemos las estadísticas (esta llamada ya debe gestionar el reseteo mensual)
        PlanUsageEntity usage = usageAuthService.getUsageStats(currentUser);

        // 1. CÁLCULO DE TOKENS (Suscripción + Bolsitas Anuales)
        int tokensUsadosMes = usage.getTokensUsedToday() != null ? usage.getTokensUsedToday() : 0;
        int limiteMensual = planDef.getMaxTokens();

        // Sumamos todas las bolsitas extra que el usuario ha comprado y no han vencido
        int tokensExtraDisponibles = usage.getTotalExtraTokensAvailable();

        // 2. VALIDACIÓN DE ESTADO PARA CONSULTAS
        int interaccionesRealizadas = usage.getQueriesUsedToday() != null ? usage.getQueriesUsedToday() : 0;
        int limiteCortesía = planDef.getMaxMonthlyInteractions();

        // Puede preguntar si:
        // a) Tiene mensajes de cortesía restantes
        // b) O tiene saldo en el plan mensual
        // c) O tiene saldo en sus bolsitas compradas
        boolean canQuery = (interaccionesRealizadas < limiteCortesía)
                || (limiteMensual == -1)
                || (tokensUsadosMes < limiteMensual)
                || (tokensExtraDisponibles > 0);

        // 3. CONSTRUCCIÓN DE LA RESPUESTA (Mapeando los nuevos Enums de Acceso)
        UsageResponseDTO response = UsageResponseDTO.builder()
                .planName(planDef.getDbName())

                // Datos de Tokens y Límites
                .tokensUsed(tokensUsadosMes)
                .tokensLimit(limiteMensual)
                .extraTokens(tokensExtraDisponibles) // Saldo total de todas sus bolsitas
                .queriesUsed(interaccionesRealizadas)
                .queriesLimit(limiteCortesía)

                .canMakeMoreQueries(canQuery)

                // MAPEOS DE ACCESO (Ahora usamos .isCanEnter() del Enum Access)
                .canUseMiniApps(planDef.getMiniAppsAccess().isCanEnter())
                .canUseProxy(planDef.getAppsAccess().isCanEnter())
                .canUseGenerator(planDef.getDocsAccess().isCanEnter()) // Juxa Docs
                .canUseConstructor(planDef.getConstructorAccess().isCanEnter()) // Juxa Constructor
                .canUseRedactor(planDef.isCanUseRedactor())// <--- Redactor de Hechos
                .canUseEducational(planDef.isCanUseExam())      // <--- Examen y Guía
                .canUseAnalysis(planDef.isCanUseEvidenceValidator())
                .canUseSemantic(planDef.isCanUseTipicidad())
                .canUseSustento(planDef.isCanUseMedidasCautelares())

                // Etiquetas de ayuda para el Frontend (Opcional, pero útil)
                .docsAccessLabel(planDef.getDocsAccess().getLabel())
                .constructorAccessLabel(planDef.getConstructorAccess().getLabel())

                // Capacidades del modelo (Siguen siendo campos directos en JuxaPlanDef)
                .aiModel(planDef.getAiModel())

                // Nota: Si quitaste estos booleanos de JuxaPlanDef,
                // aquí deberás usar constantes o eliminarlos del DTO
                .canUploadAudio(planDef.isCanUploadAudio())
                .canUploadVideo(planDef.isCanUploadVideo())
                .hasFullHistory(planDef.isHasFullHistory())


                .build();

        return ResponseEntity.ok(response);
    }
}