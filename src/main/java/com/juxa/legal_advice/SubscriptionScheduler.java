package com.juxa.legal_advice.scheduler;

import com.juxa.legal_advice.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionScheduler {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionScheduler.class);

    @Autowired
    private UserService userService;

    /**
     * Executes everyday at 00:00:01.
     * The zone is explicitly set to ensure it respects Mexico City time (CST/CDT).
     */
    @Scheduled(cron = "01 00 00 * * ?", zone = "America/Mexico_City")
    // @Scheduled(fixedRate = 5000) // Para tus pruebas
    public void checkAndDeactivateSubscriptions() {
        logger.info("Starting daily subscription check...");

        // 1. Desactiva los trials que vencieron hoy
        userService.deactivateExpiredTrialSubscriptions();

        // 2. Desactiva los activos que ya pasaron sus 7 días de gracia
        userService.deactivateExpiredActiveSubscriptions();

        logger.info("Daily subscription check finished.");
    }
}