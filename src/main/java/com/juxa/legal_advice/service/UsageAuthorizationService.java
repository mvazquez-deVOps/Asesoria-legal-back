package com.juxa.legal_advice.service;

import com.juxa.legal_advice.config.JuxaPlanDef;
import com.juxa.legal_advice.config.exceptions.PlanLimitExceededException;
import com.juxa.legal_advice.model.PlanUsageEntity;
import com.juxa.legal_advice.model.UserEntity;
import com.juxa.legal_advice.repository.PlanUsageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class UsageAuthorizationService {

    @Autowired
    private PlanUsageRepository planUsageRepository;

    final int maximoNumeroDeTokensDeRespuestaEstimados = 1500;

    /**
     * Obtiene el uso del usuario y garantiza que los contadores diarios estén reseteados si es un nuevo día.
     */
    private PlanUsageEntity getAndResetUsageIfNeeded(UserEntity user) {
        PlanUsageEntity usage = user.getPlanUsage();
        if (usage == null) {
            usage = PlanUsageEntity.builder().user(user).build();
        }

        LocalDate today = LocalDate.now();

        // Verificamos si es un día nuevo
        if (usage.getLastResetDate() == null || usage.getLastResetDate().isBefore(today)) {
            usage.setQueriesUsedToday(0);
            usage.setFilesUploadedToday(0);
            usage.setTokensUsedToday(0); // Se resetean a 0 porque es un nuevo día
            usage.setLastResetDate(today);
            usage = planUsageRepository.save(usage);
        }

        return usage;
    }

    private void checkSubscriptionStatus(UserEntity user) {
        if (user.getSubscriptionPlan().equalsIgnoreCase("FREE") &&
                user.getTrialEndDate() != null &&
                LocalDateTime.now().isAfter(user.getTrialEndDate())) {
            throw new RuntimeException("Tu periodo de prueba ha expirado. Por favor, adquiere un plan.");
        }
    }

    /**
     * FASE 1: Verificación PREVIA (Heurística)
     */
    @Transactional
    public void validateSufficientTokens(UserEntity user, String message, String extractedText, String historyJson) {
        checkSubscriptionStatus(user);

        // Al obtener el usage aquí, ya nos aseguramos de que esté reseteado si es un nuevo día
        PlanUsageEntity usage = getAndResetUsageIfNeeded(user);
        JuxaPlanDef planDef = JuxaPlanDef.fromString(user.getSubscriptionPlan());

        if (planDef.getMaxTokens() == -1) return; // Plan Ilimitado

        int tokensUsadosHoy = usage.getTokensUsedToday() != null ? usage.getTokensUsedToday() : 0;
        int tokensExtra = usage.getExtraTokens() != null ? usage.getExtraTokens() : 0;

        // Tokens disponibles = (Límite diario del plan - usados hoy) + Tokens Extra comprados
        int tokensDiariosRestantes = Math.max(0, planDef.getMaxTokens() - tokensUsadosHoy);
        int totalTokensDisponibles = tokensDiariosRestantes + tokensExtra;

        if (totalTokensDisponibles <= 0) {
            throw new PlanLimitExceededException("Has agotado tu saldo de tokens diarios y extras. Por favor, espera a mañana o compra más tokens.");
        }

        // Estimación
        int lengthEstimado = message.length() + (historyJson != null ? historyJson.length() : 0);
        if (extractedText != null && !extractedText.isEmpty()) {
            lengthEstimado += extractedText.length();
        }
        int tokensEstimados = lengthEstimado / 4;
        int costoTotalEstimado = tokensEstimados + maximoNumeroDeTokensDeRespuestaEstimados;

        if (costoTotalEstimado > totalTokensDisponibles) {
            throw new PlanLimitExceededException("La consulta es muy grande para tu saldo actual. " +
                    "Requiere. " + costoTotalEstimado + " tokens, pero solo tienes " + totalTokensDisponibles + " disponibles.");
        }
    }

    /**
     * FASE 2: Consumo POSTERIOR.
     */
    @Transactional
    public void consumeTokens(UserEntity user, int totalTokensSpent) {
        // Obtenemos el usage (con seguridad de que está en el día correcto)
        PlanUsageEntity usage = getAndResetUsageIfNeeded(user);
        JuxaPlanDef planDef = JuxaPlanDef.fromString(user.getSubscriptionPlan());

        // Si es ilimitado, no hacemos nada
        if (planDef.getMaxTokens() == -1) return;

        int currentTokensUsed = usage.getTokensUsedToday() != null ? usage.getTokensUsedToday() : 0;
        int planMaxTokens = planDef.getMaxTokens();

        // Vemos cuántos tokens le quedaban de su límite diario ANTES de este gasto
        int dailyTokensRemaining = Math.max(0, planMaxTokens - currentTokensUsed);

        if (totalTokensSpent <= dailyTokensRemaining) {
            // El gasto se cubre totalmente con el plan diario
            usage.setTokensUsedToday(currentTokensUsed + totalTokensSpent);
        } else {
            // El gasto excede el plan diario, debemos usar extra_tokens
            int tokensExcedentes = totalTokensSpent - dailyTokensRemaining;

            // Llenamos el uso diario al tope
            usage.setTokensUsedToday(planMaxTokens);

            // Restamos los excedentes de los extra_tokens
            int currentExtraTokens = usage.getExtraTokens() != null ? usage.getExtraTokens() : 0;
            usage.setExtraTokens(Math.max(0, currentExtraTokens - tokensExcedentes));
        }

        // Aumentamos el contador de queries
        int currentQueries = usage.getQueriesUsedToday() != null ? usage.getQueriesUsedToday() : 0;
        usage.setQueriesUsedToday(currentQueries + 1);

        planUsageRepository.save(usage);
    }

    // El método getUsageStats se mantiene casi igual, pero usando la nueva función centralizada
    @Transactional
    public PlanUsageEntity getUsageStats(UserEntity user) {
        return getAndResetUsageIfNeeded(user);
    }

    // Añadir a UsageAuthorizationService.java

    public int getAvailableOcrPages(UserEntity user) {
        PlanUsageEntity usage = getAndResetUsageIfNeeded(user);
        JuxaPlanDef planDef = JuxaPlanDef.fromString(user.getSubscriptionPlan());

        int limit = planDef.getMaxOcrPages();

        // Si el plan es ilimitado (-1), devolvemos el valor máximo de un Integer para que nunca falle la validación
        if (limit == -1) {
            return Integer.MAX_VALUE;
        }

        int used = usage.getFilesUploadedToday() != null ? usage.getFilesUploadedToday() : 0;
        return Math.max(0, limit - used);
    }

    @Transactional

    public void consumeOcrPages(UserEntity user, int pagesUsed) {
        if (pagesUsed <= 0) return; // Si fue gratis (Word o PDF digital), no hacemos nada

        JuxaPlanDef planDef = JuxaPlanDef.fromString(user.getSubscriptionPlan());
        if (planDef.getMaxOcrPages() == -1) return; // Si es ilimitado, no gastamos tiempo guardando el uso

        PlanUsageEntity usage = getAndResetUsageIfNeeded(user);
        int current = usage.getFilesUploadedToday() != null ? usage.getFilesUploadedToday() : 0;
        usage.setFilesUploadedToday(current + pagesUsed);

        planUsageRepository.save(usage);
    }
}

