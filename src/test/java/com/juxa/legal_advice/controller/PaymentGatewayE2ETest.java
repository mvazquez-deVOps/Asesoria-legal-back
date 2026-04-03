package com.juxa.legal_advice.controller;

import com.juxa.legal_advice.model.PlanEntity;
import com.juxa.legal_advice.model.SubscriptionEntity;
import com.juxa.legal_advice.model.UserEntity;
import com.juxa.legal_advice.repository.PlanRepository;
import com.juxa.legal_advice.repository.SubscriptionRepository;
import com.juxa.legal_advice.repository.UserRepository;
import com.stripe.model.Customer;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // Revierte los cambios en la BD al terminar cada test
public class PaymentGatewayE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        // 1. CREAR O RECUPERAR EL USUARIO
        testUser = userRepository.findByEmail("usuario_e2e@juxa.com").orElseGet(() -> {
            UserEntity newUser = new UserEntity();
            newUser.setEmail("usuario_e2e@juxa.com");
            newUser.setName("Usuario E2E");
            newUser.setPassword("password_segura");
            newUser.setIsVerified(true);
            // IMPORTANTE: Le asignamos un Customer ID para que el Portal funcione
            newUser.setStripeCustomerId("cus_simulado_123");
            return userRepository.save(newUser);
        });

        // 2. CREAR O RECUPERAR EL PLAN BASE
        PlanEntity planBase = planRepository.findByName("juxa_go").orElseGet(() -> {
            PlanEntity newPlan = new PlanEntity();
            newPlan.setName("juxa_go");
            newPlan.setStripePriceId("price_juxago_test");
            return planRepository.save(newPlan);
        });

        // 3. CREAR O RECUPERAR EL PLAN DE TOKENS
        planRepository.findByName("tokens_500").orElseGet(() -> {
            PlanEntity tokenPlan = new PlanEntity();
            tokenPlan.setName("tokens_500");
            tokenPlan.setStripePriceId("price_tokens_500_test");
            return planRepository.save(tokenPlan);
        });

        // 4. CREAR SUSCRIPCIÓN ACTIVA (Necesaria para probar cancelaciones)
        subscriptionRepository.findByUserId(testUser.getId()).orElseGet(() -> {
            SubscriptionEntity sub = new SubscriptionEntity();
            sub.setUser(testUser);
            sub.setPlan(planBase);
            sub.setStripeSubscriptionId("sub_simulada_456");
            sub.setStatus("active");
            sub.setCurrentPeriodStart(LocalDateTime.now().minusDays(10));
            sub.setCurrentPeriodEnd(LocalDateTime.now().plusDays(20));
            sub.setCancelAtPeriodEnd(false);
            return subscriptionRepository.save(sub);
        });
    }

    // ==========================================
    // TEST 1: COMPRA EXTRA DE 500 TOKENS
    // ==========================================
    @Test
    @WithMockUser(username = "usuario_e2e@juxa.com")
    void endToEnd_BuyExtraTokensFlow() throws Exception {
        try (MockedStatic<Customer> customerMock = Mockito.mockStatic(Customer.class);
             MockedStatic<Session> sessionMock = Mockito.mockStatic(Session.class)) {

            Session mockSession = Mockito.mock(Session.class);
            Mockito.when(mockSession.getUrl()).thenReturn("https://checkout.stripe.com/pay/tokens500");

            sessionMock.when(() -> Session.create((com.stripe.param.checkout.SessionCreateParams) Mockito.any()))
                    .thenReturn(mockSession);

            mockMvc.perform(post("/api/payments/buy-extra-500-tokens"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.url").value("https://checkout.stripe.com/pay/tokens500"));
        }
    }

    // ==========================================
    // TEST 2: CHECKOUT DE PRUEBA (TRIAL)
    // ==========================================
    @Test
    @WithMockUser(username = "usuario_e2e@juxa.com")
    void endToEnd_CreateTrialCheckout() throws Exception {
        try (MockedStatic<Session> sessionMock = Mockito.mockStatic(Session.class)) {

            Session mockSession = Mockito.mock(Session.class);
            Mockito.when(mockSession.getUrl()).thenReturn("https://checkout.stripe.com/pay/trial");

            sessionMock.when(() -> Session.create((com.stripe.param.checkout.SessionCreateParams) Mockito.any()))
                    .thenReturn(mockSession);

            mockMvc.perform(post("/api/payments/create-trial-checkout"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.url").value("https://checkout.stripe.com/pay/trial"));
        }
    }

    // ==========================================
    // TEST 3: CHECKOUT ESTÁNDAR DE SUSCRIPCIÓN
    // ==========================================
    @Test
    @WithMockUser(username = "usuario_e2e@juxa.com")
    void endToEnd_CreateStandardCheckout() throws Exception {

        // 👇 SOLUCIÓN: Eliminar la suscripción que se creó en el setUp()
        // para simular que es un usuario nuevo o sin plan activo.
        subscriptionRepository.deleteAll();

        try (MockedStatic<Session> sessionMock = Mockito.mockStatic(Session.class)) {

            Session mockSession = Mockito.mock(Session.class);
            Mockito.when(mockSession.getUrl()).thenReturn("https://checkout.stripe.com/pay/standard");
            Mockito.when(mockSession.getId()).thenReturn("cs_simulado_789");

            sessionMock.when(() -> Session.create((com.stripe.param.checkout.SessionCreateParams) Mockito.any()))
                    .thenReturn(mockSession);

            String payload = "{\"category\":\"juxa_go\"}";

            mockMvc.perform(post("/api/payments/checkout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.url").value("https://checkout.stripe.com/pay/standard"))
                    .andExpect(jsonPath("$.sessionId").value("cs_simulado_789"));
        }
    }

    // ==========================================
    // TEST 4: CREACIÓN DEL PORTAL DEL CLIENTE
    // ==========================================
    @Test
    @WithMockUser(username = "usuario_e2e@juxa.com")
    void endToEnd_CreatePortalSession() throws Exception {
        // Cuidado aquí: Este Session es el del portal, NO el del checkout
        try (MockedStatic<com.stripe.model.billingportal.Session> portalSessionMock =
                     Mockito.mockStatic(com.stripe.model.billingportal.Session.class)) {

            com.stripe.model.billingportal.Session mockPortal = Mockito.mock(com.stripe.model.billingportal.Session.class);
            Mockito.when(mockPortal.getUrl()).thenReturn("https://billing.stripe.com/portal/test");

            portalSessionMock.when(() -> com.stripe.model.billingportal.Session.create((com.stripe.param.billingportal.SessionCreateParams) Mockito.any()))
                    .thenReturn(mockPortal);

            mockMvc.perform(post("/api/payments/portal"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.url").value("https://billing.stripe.com/portal/test"));
        }
    }

    // ==========================================
    // TEST 5: CANCELAR SUSCRIPCIÓN ACTIVA
    // ==========================================
    @Test
    @WithMockUser(username = "usuario_e2e@juxa.com")
    void endToEnd_CancelSubscription() throws Exception {
        try (MockedStatic<Subscription> subscriptionMock = Mockito.mockStatic(Subscription.class)) {

            // Configuramos el mock para la respuesta de Stripe
            Subscription mockStripeSub = Mockito.mock(Subscription.class);
            // Cuando intente recuperar "sub_simulada_456" (que insertamos en el BeforeEach)
            subscriptionMock.when(() -> Subscription.retrieve("sub_simulada_456")).thenReturn(mockStripeSub);

            // Simular el proceso de actualización (update)
            Mockito.when(mockStripeSub.update((Map<String, Object>) Mockito.any())).thenReturn(mockStripeSub);

            mockMvc.perform(post("/api/payments/cancel-subscription"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").exists());
            // Puedes validar el texto exacto, pero verificar que existe suele ser suficiente si incluye fechas dinámicas
        }
    }

    // ==========================================
    // TEST 6: COMPRA DINÁMICA DE TOKENS (CASO ERROR)
    // ==========================================
    @Test
    @WithMockUser(username = "usuario_e2e@juxa.com")
    void endToEnd_CreateDynamicTokensCheckout_InvalidPackage() throws Exception {
        // Probamos que el controlador protege bien si el frontend envía un plan que no existe en el Enum
        String payload = "{\"packageName\":\"HACK_INTENTO_PAQUETE_FALSO\"}";

        mockMvc.perform(post("/api/payments/checkout/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                // 👇 Actualizado al nuevo formato de GlobalExceptionHandler
                .andExpect(jsonPath("$.error").value("Petición Inválida"));
    }

    // ==========================================
    // TEST 7: BLOQUEAR COMPRA SI YA TIENE SUSCRIPCIÓN ACTIVA
    // ==========================================
    @Test
    @WithMockUser(username = "usuario_e2e@juxa.com")
    void endToEnd_CreateStandardCheckout_FailsIfAlreadySubscribed() throws Exception {

        String payload = "{\"category\":\"juxa_go\"}";

        mockMvc.perform(post("/api/payments/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                // 👇 SOLUCIÓN: Cambiar "$.error" por "$.message"
                .andExpect(jsonPath("$.message").value("El usuario ya tiene una suscripción activa. Para modificarla, utiliza el Portal de Cliente."));
    }
}