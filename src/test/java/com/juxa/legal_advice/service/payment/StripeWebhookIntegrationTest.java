package com.juxa.legal_advice.service.payment;

import com.juxa.legal_advice.model.PlanUsageEntity;
import com.juxa.legal_advice.model.UserEntity;
import com.juxa.legal_advice.repository.PlanUsageRepository;
import com.juxa.legal_advice.repository.UserRepository;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional // Rollback automático al terminar
public class StripeWebhookIntegrationTest {

    @Autowired
    private StripeWebhookService webhookService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlanUsageRepository planUsageRepository;

    private MockedStatic<Webhook> webhookMockedStatic;
    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        // Creamos un usuario real en la base de datos de prueba
        testUser = new UserEntity();
        testUser.setEmail("tokens@juxa.com");
        testUser.setPassword("pass");
        testUser.setIsVerified(true);
        testUser = userRepository.save(testUser);

        // Activamos el mock estático para la clase Webhook de Stripe
        webhookMockedStatic = Mockito.mockStatic(Webhook.class);
    }

    @AfterEach
    void tearDown() {
        // Es vital cerrar el mock estático después de cada test
        webhookMockedStatic.close();
    }

    @Test
    void handleWebhook_ExtraTokensPurchase_ShouldUpdatePlanUsage() throws Exception {
        // 1. Preparar el objeto Session (Checkout) simulado
        Session mockSession = mock(Session.class);
        when(mockSession.getPaymentStatus()).thenReturn("paid");
        when(mockSession.getMetadata()).thenReturn(Map.of(
                "payment_type", "extra_tokens",
                "user_id", String.valueOf(testUser.getId()),
                "token_amount", "500"
        ));

        // 2. Preparar el Evento de Stripe simulado
        Event mockEvent = mock(Event.class);
        when(mockEvent.getId()).thenReturn("evt_test_123");
        when(mockEvent.getType()).thenReturn("checkout.session.completed");

        EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
        when(deserializer.getObject()).thenReturn(Optional.of(mockSession));
        when(mockEvent.getDataObjectDeserializer()).thenReturn(deserializer);

        // 3. Forzar a Webhook.constructEvent a devolver nuestro evento falso (bypasseando la firma)
        webhookMockedStatic.when(() -> Webhook.constructEvent(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(mockEvent);

        // 4. Ejecutar el servicio
        String result = webhookService.handleWebhook("payload_falso", "firma_falsa");

        // 5. Verificar base de datos y respuesta
        assertEquals("Compra de tokens procesada exitosamente", result);

        PlanUsageEntity updatedUsage = planUsageRepository.findByUserId(testUser.getId()).get();
        assertEquals(500, updatedUsage.getExtraTokens(), "Los tokens extra deben sumarse en la base de datos");
    }
}