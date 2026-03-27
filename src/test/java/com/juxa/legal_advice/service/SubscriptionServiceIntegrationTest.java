package com.juxa.legal_advice.service;

import com.juxa.legal_advice.model.PlanEntity;
import com.juxa.legal_advice.model.SubscriptionEntity;
import com.juxa.legal_advice.model.UserEntity;
import com.juxa.legal_advice.repository.PlanRepository;
import com.juxa.legal_advice.repository.SubscriptionRepository;
import com.juxa.legal_advice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional // IMPORTANTE: Hace un "rollback" automático al terminar cada test
public class SubscriptionServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private PlanRepository planRepository; // Asumo que se llama así tu repositorio

    private PlanEntity testPlan;
    private UserEntity testUser;
    private SubscriptionEntity testSubscription;

    @BeforeEach
    void setUp() {
        // 1. Creamos y guardamos el Usuario
        testUser = new UserEntity();
        testUser.setEmail("test.scheduler@ejemplo.com");
        testUser.setSubscriptionPlan("juxa_go");
        testUser.setPassword("password_falsa_123");
        testUser.setName("Usuario de Prueba");
        testUser.setIsVerified(true);
        testUser = userRepository.save(testUser);

        // 2. Creamos y guardamos el Plan de prueba (Evita el error de Foreign Key)
        testPlan = new PlanEntity();
        testPlan.setName("juxa_go");
        testPlan.setStripePriceId("price_test_12345"); // Tiene que ser unique según tu DB
        testPlan = planRepository.save(testPlan);

        // 3. Creamos la Suscripción base
        testSubscription = new SubscriptionEntity();
        testSubscription.setUser(testUser);

        // --- AQUÍ ESTÁ LA MAGIA --- Le pasamos el objeto plan, no un ID suelto
        testSubscription.setPlan(testPlan);

        testSubscription.setStripeSubscriptionId("sub_test_123");

        // --- AQUÍ ARREGLAMOS EL ERROR DE "current_period_start cannot be null" ---
        testSubscription.setCurrentPeriodStart(LocalDateTime.now().minusDays(30));
        testSubscription.setCurrentPeriodEnd(LocalDateTime.now().plusDays(1));
    }

    @Test
    void testActiveSubscription_3DaysExpired_ShouldNotBeDeactivated() {
        // 1. Preparar datos: Venció hace 3 días (Aún en periodo de gracia)
        testSubscription.setStatus("active");
        testSubscription.setCurrentPeriodEnd(LocalDateTime.now().minusDays(3));
        subscriptionRepository.save(testSubscription);

        // 2. Ejecutar el Job
        userService.deactivateExpiredActiveSubscriptions();

        // 3. Verificar resultados
        UserEntity updatedUser = userRepository.findById(testUser.getId()).get();
        SubscriptionEntity updatedSub = subscriptionRepository.findById(testSubscription.getId()).get();

        assertEquals("juxa_go", updatedUser.getSubscriptionPlan(), "El plan no debe cambiar, está en periodo de gracia");
        assertEquals("active", updatedSub.getStatus(), "El status debe seguir activo");
    }

    @Test
    void testActiveSubscription_10DaysExpired_ShouldBeDeactivated() {
        // 1. Preparar datos: Venció hace 10 días (Fuera del periodo de gracia)
        testSubscription.setStatus("active");
        testSubscription.setCurrentPeriodEnd(LocalDateTime.now().minusDays(10));
        subscriptionRepository.save(testSubscription);

        // 2. Ejecutar el Job
        userService.deactivateExpiredActiveSubscriptions();

        // 3. Verificar resultados
        UserEntity updatedUser = userRepository.findById(testUser.getId()).get();
        SubscriptionEntity updatedSub = subscriptionRepository.findById(testSubscription.getId()).get();

        assertEquals("FREE", updatedUser.getSubscriptionPlan(), "El plan debe cambiar a FREE");
        assertEquals("inactive", updatedSub.getStatus(), "El status debe cambiar a inactive");
    }

    @Test
    void testTrialingSubscription_1DayExpired_ShouldBeDeactivatedImmediately() {
        // 1. Preparar datos: Trial vencido ayer (No tiene periodo de gracia)
        testSubscription.setStatus("trialing");
        testSubscription.setCurrentPeriodEnd(LocalDateTime.now().minusDays(1));
        subscriptionRepository.save(testSubscription);

        // 2. Ejecutar el Job de Trials
        userService.deactivateExpiredTrialSubscriptions();

        // 3. Verificar resultados
        UserEntity updatedUser = userRepository.findById(testUser.getId()).get();
        SubscriptionEntity updatedSub = subscriptionRepository.findById(testSubscription.getId()).get();

        assertEquals("FREE", updatedUser.getSubscriptionPlan(), "El plan debe cambiar a FREE inmediatamente al terminar el trial");
        assertEquals("inactive", updatedSub.getStatus(), "El status de trial debe cambiar a inactive");
    }
}