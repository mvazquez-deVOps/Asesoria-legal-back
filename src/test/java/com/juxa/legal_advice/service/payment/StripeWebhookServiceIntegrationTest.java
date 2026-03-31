package com.juxa.legal_advice.service.payment;

import com.juxa.legal_advice.model.PaymentHistoryEntity;
import com.juxa.legal_advice.model.PlanEntity;
import com.juxa.legal_advice.model.PlanUsageEntity;
import com.juxa.legal_advice.model.SubscriptionEntity;
import com.juxa.legal_advice.model.UserEntity;
import com.juxa.legal_advice.repository.*;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Invoice;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional // Revierte la base de datos después de cada test
public class StripeWebhookServiceIntegrationTest {

    @Autowired
    private StripeWebhookService webhookService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private PlanUsageRepository planUsageRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private PaymentHistoryRepository paymentHistoryRepository;

    @Autowired
    private ProcessedStripeEventRepository processedEventRepository;

    private MockedStatic<Webhook> webhookMockedStatic;
    private UserEntity testUser;
    private PlanEntity testPlan;

    @BeforeEach
    void setUp() {
        // 1. Crear usuario de prueba en la BD
        testUser = userRepository.findByEmail("webhook_user@juxa.com").orElseGet(() -> {
            UserEntity u = new UserEntity();
            u.setEmail("webhook_user@juxa.com");
            u.setPassword("pass");
            u.setIsVerified(true);
            return userRepository.save(u);
        });

        // 2. Crear plan de prueba en la BD
        testPlan = planRepository.findByName("tokens_500").orElseGet(() -> {
            PlanEntity p = new PlanEntity();
            p.setName("tokens_500");
            p.setStripePriceId("price_tokens_500");
            return planRepository.save(p);
        });

        // 3. Interceptar Webhook.constructEvent para saltar la validación criptográfica
        webhookMockedStatic = Mockito.mockStatic(Webhook.class);
    }

    @AfterEach
    void tearDown() {
        webhookMockedStatic.close();
    }

    // ==========================================
    // TEST 1: COMPRA DE TOKENS (Session Completed)
    // ==========================================
    @Test
    void testHandleWebhook_ExtraTokens_ShouldAddTokensToDB() throws Exception {
        // 1. Configuramos el Session falso
        Session mockSession = Mockito.mock(Session.class);
        Mockito.when(mockSession.getPaymentStatus()).thenReturn("paid");
        Mockito.when(mockSession.getMetadata()).thenReturn(Map.of(
                "payment_type", "extra_tokens",
                "user_id", String.valueOf(testUser.getId()),
                "token_amount", "500"
        ));

        // 2. Configuramos el Evento falso
        Event mockEvent = createMockEvent("evt_tokens_123", "checkout.session.completed", mockSession);

        // 3. Ejecutamos el servicio
        String result = webhookService.handleWebhook("payload_falso", "firma_falsa");

        // 4. Verificaciones
        assertEquals("Compra de tokens procesada exitosamente", result);

        PlanUsageEntity usage = planUsageRepository.findByUserId(testUser.getId()).orElseThrow();
        assertEquals(500, usage.getExtraTokens(), "Los tokens debieron sumarse al usuario en la BD.");
    }

    // ==========================================
    // TEST 2: PAGO DE FACTURA (Invoice Paid)
    // ==========================================
    @Test
    void testHandleWebhook_InvoicePaid_ShouldSavePaymentHistory() throws Exception {
        // 1. Creamos una suscripción en la BD para que la factura tenga a quién asignarse
        SubscriptionEntity sub = new SubscriptionEntity();
        sub.setUser(testUser);
        sub.setPlan(testPlan);
        sub.setStripeSubscriptionId("sub_real_123");
        sub.setStatus("active");
        sub.setCurrentPeriodStart(java.time.LocalDateTime.now());
        sub.setCurrentPeriodEnd(java.time.LocalDateTime.now().plusDays(30));
        subscriptionRepository.save(sub);

        // 2. Configuramos un Invoice Falso usando RETURNS_DEEP_STUBS (¡Magia negra de Mockito!)
        // Esto permite hacer invoice.getLines().getData().get(0).getSubscription() sin que explote.
        Invoice mockInvoice = Mockito.mock(Invoice.class, Answers.RETURNS_DEEP_STUBS);
        Mockito.when(mockInvoice.getId()).thenReturn("in_test_123");
        Mockito.when(mockInvoice.getAmountPaid()).thenReturn(1500L); // 15.00 en Stripe
        Mockito.when(mockInvoice.getStatus()).thenReturn("paid");
        // Simulamos la anidación profunda de Stripe
        Mockito.when(mockInvoice.getLines().getData().get(0).getSubscription()).thenReturn("sub_real_123");

        Event mockEvent = createMockEvent("evt_invoice_456", "invoice.paid", mockInvoice);

        // 3. Ejecutamos
        String result = webhookService.handleWebhook("payload", "firma");

        // 4. Verificaciones
        assertEquals("Pago exitoso registrado", result);

        Optional<PaymentHistoryEntity> payment = paymentHistoryRepository.findByStripeInvoiceId("in_test_123");
        assertTrue(payment.isPresent(), "El historial de pago debió guardarse.");
        assertEquals(new BigDecimal("15"), payment.get().getAmountPaid(), "El monto debe dividirse entre 100.");
        assertEquals("paid", payment.get().getStatus());
    }

    // ==========================================
    // TEST 3: EVENTO DUPLICADO (Idempotencia)
    // ==========================================
    @Test
    void testHandleWebhook_DuplicateEvent_ShouldIgnore() throws Exception {
        // 1. Configuramos un evento falso
        Event mockEvent = createMockEvent("evt_duplicado_789", "invoice.paid", Mockito.mock(Invoice.class));

        // 2. Ejecutamos la primera vez (Se procesará o dará error por falta de datos, no importa para este test)
        try {
            webhookService.handleWebhook("payload", "firma");
        } catch (Exception ignored) {}

        assertTrue(processedEventRepository.existsById("evt_duplicado_789"), "El evento debió registrarse como procesado.");

        // 3. Ejecutamos la SEGUNDA vez con el mismo ID de evento
        String result = webhookService.handleWebhook("payload", "firma");

        // 4. Verificamos que lo ignoró y no hizo nada
        assertEquals("Evento duplicado ignorado exitosamente", result);
    }

    // ==========================================
    // MÉTODO AUXILIAR PARA CREAR EVENTOS
    // ==========================================
    private Event createMockEvent(String eventId, String eventType, Object stripeObject) throws SignatureVerificationException {
        Event mockEvent = Mockito.mock(Event.class);
        Mockito.when(mockEvent.getId()).thenReturn(eventId);
        Mockito.when(mockEvent.getType()).thenReturn(eventType);

        // Simulamos la deserialización del objeto interno de Stripe (Session, Invoice, etc.)
        EventDataObjectDeserializer deserializer = Mockito.mock(EventDataObjectDeserializer.class);
        Mockito.when(deserializer.getObject()).thenReturn(Optional.of((com.stripe.model.StripeObject) stripeObject));
        Mockito.when(mockEvent.getDataObjectDeserializer()).thenReturn(deserializer);

        // Le decimos a nuestro mock estático que devuelva este evento
        webhookMockedStatic.when(() -> Webhook.constructEvent(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(mockEvent);

        return mockEvent;
    }
}