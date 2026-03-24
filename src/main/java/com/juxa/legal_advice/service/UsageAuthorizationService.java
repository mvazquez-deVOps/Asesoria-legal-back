package com.juxa.legal_advice.service;

import com.juxa.legal_advice.config.JuxaPlanDef;
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

    /**
     * Verifica y actualiza el uso de una consulta (Chat/IA)
     */
    @Transactional
    public void authorizeAndConsumeQuery(UserEntity user) {
        checkSubscriptionStatus(user);
        PlanUsageEntity usage = getOrCreateUsage(user);
        resetCountersIfNeeded(usage);

        JuxaPlanDef planDef = JuxaPlanDef.fromString(user.getSubscriptionPlan());

        // -1 significa ilimitado
        if (planDef.getMaxQueriesPerDay() != -1 && usage.getQueriesUsedToday() >= planDef.getMaxQueriesPerDay()) {
            throw new RuntimeException("Has alcanzado el límite diario de consultas de tu plan (" + planDef.getMaxQueriesPerDay() + ").");
        }

        // Si pasa la validación, consumimos 1
        usage.setQueriesUsedToday(usage.getQueriesUsedToday() + 1);
        planUsageRepository.save(usage);
    }

    /**
     * Verifica y actualiza el uso de subida de archivos
     */
    @Transactional
    public void authorizeAndConsumeFileUpload(UserEntity user) {
        checkSubscriptionStatus(user);
        PlanUsageEntity usage = getOrCreateUsage(user);
        resetCountersIfNeeded(usage);

        JuxaPlanDef planDef = JuxaPlanDef.fromString(user.getSubscriptionPlan());

        if (planDef.getMaxFilesPerDay() != -1 && usage.getFilesUploadedToday() >= planDef.getMaxFilesPerDay()) {
            throw new RuntimeException("Has alcanzado el límite diario de subida de archivos (" + planDef.getMaxFilesPerDay() + ").");
        }

        usage.setFilesUploadedToday(usage.getFilesUploadedToday() + 1);
        planUsageRepository.save(usage);
    }

    // --- Métodos Auxiliares ---

    private void checkSubscriptionStatus(UserEntity user) {
        // Aquí verificas si su trial expiró y si no es un plan de pago.
        if (user.getSubscriptionPlan().equalsIgnoreCase("FREE") &&
                user.getTrialEndDate() != null &&
                LocalDateTime.now().isAfter(user.getTrialEndDate())) {
            throw new RuntimeException("Tu periodo de prueba ha expirado. Por favor, adquiere un plan.");
        }
    }

    private PlanUsageEntity getOrCreateUsage(UserEntity user) {
        if (user.getPlanUsage() != null) {
            return user.getPlanUsage();
        }
        PlanUsageEntity newUsage = PlanUsageEntity.builder().user(user).build();
        return planUsageRepository.save(newUsage);
    }

    private void resetCountersIfNeeded(PlanUsageEntity usage) {
        LocalDate today = LocalDate.now();
        if (usage.getLastResetDate() == null || usage.getLastResetDate().isBefore(today)) {
            usage.setQueriesUsedToday(0);
            usage.setFilesUploadedToday(0);
            usage.setLastResetDate(today);
        }
    }

    @Transactional
    public PlanUsageEntity getUsageStats(UserEntity user) {
        // Usamos la función auxiliar que creamos anteriormente
        PlanUsageEntity usage = getOrCreateUsage(user);

        LocalDate today = LocalDate.now();

        // Si nunca se ha reseteado o si la última fecha es anterior a hoy
        if (usage.getLastResetDate() == null || usage.getLastResetDate().isBefore(today)) {
            usage.setQueriesUsedToday(0);
            usage.setFilesUploadedToday(0);
            usage.setLastResetDate(today);

            // Guardamos el reseteo en la base de datos
            usage = planUsageRepository.save(usage);
        }

        return usage;
    }

}