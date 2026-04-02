package com.juxa.legal_advice.service;

import com.juxa.legal_advice.config.JuxaPlanDef;
import com.juxa.legal_advice.config.exceptions.PlanLimitExceededException;
import com.juxa.legal_advice.config.exceptions.TokenConfirmationRequiredException;
import com.juxa.legal_advice.model.PlanUsageEntity;
import com.juxa.legal_advice.model.UserEntity;
import com.juxa.legal_advice.repository.PlanUsageRepository;
import com.juxa.legal_advice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class UsageAuthorizationService {

    @Autowired
    private PlanUsageRepository planUsageRepository;

    @Autowired
    private UserRepository userRepository;

    final int maximoNumeroDeTokensDeRespuestaEstimados = 1500;

    /**
     * Obtiene el uso del usuario y garantiza que los contadores diarios estén reseteados si es un nuevo día.
     */
    private PlanUsageEntity getAndResetUsageIfNeeded(UserEntity user) {
        PlanUsageEntity usage = user.getPlanUsage();
        if (usage == null) {
            usage = PlanUsageEntity.builder().user(user).build();
            user.setPlanUsage(usage);
        }

        LocalDate today = LocalDate.now();
        LocalDate lastReset = usage.getLastResetDate();

        boolean isNewMonth = lastReset == null ||
                             lastReset.getMonthValue() != today.getMonthValue() ||
                             lastReset.getYear() != today.getYear();


        if (isNewMonth) {
            // Reiniciamos bolsa mensual del plan e interacciones mensuales
            usage.setQueriesUsedToday(0); // Ahora actúa como "Interacciones del Mes"
            usage.setTokensUsedToday(0);  // Ahora actúa como "Tokens del Mes"
            usage.setFilesUploadedToday(0);
            usage.setLastResetDate(today);
            usage = planUsageRepository.save(usage);

            // También reseteamos el contador "perdido" de UserEntity por si acaso
            user.setDailyMessageCount(0);
            user.setLastMessageDate(today);
            userRepository.save(user);
        }

        return usage;
    }

    private void applyTokenDeduction(PlanUsageEntity usage, int planLimit, int spent) {
        int currentUsed = usage.getTokensUsedToday();
        int remainingInPlan = Math.max(0, planLimit - currentUsed);

        if (spent <= remainingInPlan) {
            // Se descuenta del plan mensual
            usage.setTokensUsedToday(currentUsed + spent);
        } else {
            // Se agota el plan y el resto va a la bolsa extra
            int excedente = spent - remainingInPlan;
            usage.setTokensUsedToday(planLimit);
            int currentExtra = usage.getExtraTokens() != null ? usage.getExtraTokens() : 0;
            usage.setExtraTokens(Math.max(0, currentExtra - excedente));
        }
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
    public void validateSufficientTokens(UserEntity user, String module, int estimatedTokens, boolean isConfirmedByUser, String toolName) {
        JuxaPlanDef plan = JuxaPlanDef.fromString(user.getSubscriptionPlan()); //
        PlanUsageEntity usage = getAndResetUsageIfNeeded(user); //
        JuxaPlanDef.Access access = plan.getAccessForModule(module); //

        boolean isRedactor = "redactor-hechos".equalsIgnoreCase(toolName);
        boolean isEdu = "exam".equalsIgnoreCase(toolName) || "guide".equalsIgnoreCase(toolName);

        // Si es FREE y es Redactor -> GRATIS
        boolean freeIncluded = (plan == JuxaPlanDef.FREE && isRedactor);
        // Si es ESTUDIANTES y es Redactor, Examen o Guía -> GRATIS
        boolean eduIncluded = (plan == JuxaPlanDef.ESTUDIANTES && (isRedactor || isEdu));

        if (freeIncluded || eduIncluded || access == JuxaPlanDef.Access.UNLIMITED) {
            return; // No validamos tokens, tiene pase VIP para esta herramienta
        }

        // CASO A: ILIMITADO
        if (access == JuxaPlanDef.Access.UNLIMITED) return;

        // CASO B: BLOQUEADO (Aduana Flexible)
        // Permitimos el acceso a módulos bloqueados si el usuario ya confirmó gastar de su bolsa extra.
        if (access == JuxaPlanDef.Access.LOCKED && !isConfirmedByUser) {
            throw new PlanLimitExceededException("El módulo " + module + " no está incluido en tu plan actual.");
        }

        // --- CÁLCULO DE SALDOS CON TUS VARIABLES REALES ---
        // 1. Saldo del Plan Mensual
        int limitePlan = plan.getMaxTokens(); //
        int usadoPlan = usage.getTokensUsedToday() != null ? usage.getTokensUsedToday() : 0; //
        int saldoPlan = (limitePlan == -1) ? Integer.MAX_VALUE : Math.max(0, limitePlan - usadoPlan);

        // 2. Saldo de Bolsa Extra (Lo que compró o tiene de regalo)
        int saldoExtra = usage.getTotalExtraTokensAvailable(); //

        int saldoTotal = saldoPlan + saldoExtra;

        // --- REGLA ESPECIAL PARA CHAT (Cortesía) ---
        if ("CHAT".equalsIgnoreCase(module)) {
            int interacciones = usage.getQueriesUsedToday() != null ? usage.getQueriesUsedToday() : 0; //
            if (interacciones < plan.getMaxMonthlyInteractions()) {
                return; // Pasa gratis
            }
        }

        // --- VALIDACIÓN DE SALDO TOTAL (Disparador de Redirección) ---
        if (saldoTotal < estimatedTokens) {
            // IMPORTANTE: Este mensaje exacto es el que tu Front busca para abrir el cuadro de compra.
            throw new PlanLimitExceededException(
                    "¿Te acabaste tus tokens? No te preocupes, puedes comprar más para seguir usando " + module + "."
            );
        }

        // --- REGLA DE CONFIRMACIÓN ---
        // Si tiene saldo pero no ha confirmado, lanzamos el aviso para que el Front muestre el window.confirm
        if (!isConfirmedByUser) {
            throw new TokenConfirmationRequiredException(
                    "Esta respuesta usará aprox. " + estimatedTokens + " tokens de tu bolsa. ¿Deseas continuar?"
            );
        }
    }
    /**
     * FASE 2: Consumo POSTERIOR.
     */
    @Transactional
    public void consumeTokens(UserEntity user, String module, int tokensSpent, String toolName) {
        // 1. Obtenemos el uso actual (reseteando si es mes nuevo)
        PlanUsageEntity usage = getAndResetUsageIfNeeded(user);
        JuxaPlanDef planDef = JuxaPlanDef.fromString(user.getSubscriptionPlan());

        // 2. Aumentamos el contador de interacciones mensuales
        int currentQueries = usage.getQueriesUsedToday() != null ? usage.getQueriesUsedToday() : 0;
        usage.setQueriesUsedToday(currentQueries + 1);

        // 2. ¿Es una herramienta incluida gratis en el plan?
        boolean isRedactor = "redactor-hechos".equalsIgnoreCase(toolName);
        boolean isEdu = "exam".equalsIgnoreCase(toolName) || "guide".equalsIgnoreCase(toolName);

        boolean toolIsFree = (planDef == JuxaPlanDef.FREE && isRedactor) ||
                (planDef == JuxaPlanDef.ESTUDIANTES && (isRedactor || isEdu));

        // 3. ¿Corresponde cobrar tokens?
        // (Si es CHAT y ya pasó la cortesía, o si es cualquier otro módulo como APPS/DOCS)
        boolean debeCobrar = !toolIsFree && (
                !"CHAT".equalsIgnoreCase(module) ||
                        usage.getQueriesUsedToday() > planDef.getMaxMonthlyInteractions()
        );

        if (debeCobrar && planDef.getMaxTokens() != -1) {
            int currentUsed = usage.getTokensUsedToday() != null ? usage.getTokensUsedToday() : 0;
            int planLimit = planDef.getMaxTokens();
            int remainingInPlan = Math.max(0, planLimit - currentUsed);

            if (tokensSpent <= remainingInPlan) {
                // Caso A: Se descuenta del plan mensual
                usage.setTokensUsedToday(currentUsed + tokensSpent);
            } else {
                // Caso B: Se agota el plan y el resto va a la bolsa "extra_tokens" (la de 1 año)
                int excedente = tokensSpent - remainingInPlan;
                usage.setTokensUsedToday(planLimit);

                int currentExtra = usage.getExtraTokens() != null ? usage.getExtraTokens() : 0;
                usage.setExtraTokens(Math.max(0, currentExtra - excedente));
            }
        }

        planUsageRepository.save(usage);
    }

    // El método getUsageStats se mantiene casi igual, pero usando la nueva función centralizada
    @Transactional
    public PlanUsageEntity getUsageStats(UserEntity user) {
        return getAndResetUsageIfNeeded(user);
    }


    /**
     * Devuelve el saldo actual de la bolsa de tokens extra (la que no expira cada mes).
     */
    public long getExtraTokens(UserEntity user) {
        PlanUsageEntity usage = getAndResetUsageIfNeeded(user);
        return usage.getExtraTokens() != null ? usage.getExtraTokens() : 0L;
    }

    /**
     * Verifica de forma rápida si el usuario tiene saldo en su bolsa de extras.
     * Útil para decidir si mostramos el cartel de confirmación o bloqueamos directo.
     */
    public boolean hasExtraTokens(UserEntity user) {
        return getExtraTokens(user) > 0;
    }





}