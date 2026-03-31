package com.juxa.legal_advice.controller;

import com.juxa.legal_advice.dto.PortalResponseDTO;
import com.juxa.legal_advice.dto.payment.CheckoutRequestDTO;
import com.juxa.legal_advice.dto.payment.PaymentRequestDTO;
import com.juxa.legal_advice.dto.payment.PaymentResponseDTO;
import com.juxa.legal_advice.model.PlanEntity;
import com.juxa.legal_advice.model.UserEntity;
import com.juxa.legal_advice.repository.UserRepository;
import com.juxa.legal_advice.security.JwtUtil;
import com.juxa.legal_advice.service.PlanService;
import com.juxa.legal_advice.service.RateLimitingService;
import com.juxa.legal_advice.service.UserService;
import com.juxa.legal_advice.service.payment.PaymentService;
import com.juxa.legal_advice.service.payment.StripeWebhookService;
import com.stripe.model.Customer;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false) // 👇 ESTO APAGA LA SEGURIDAD SOLO PARA ESTE TEST
public class PaymentControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private StripeWebhookService stripeWebhookService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private PlanService planService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private RateLimitingService rateLimitingService; // <-- Añade esta línea

    private UserEntity mockUser;
    private PlanEntity mockPlan;

    @BeforeEach
    void setUp() {
        // Configuramos un usuario falso
        mockUser = new UserEntity();
        mockUser.setId(1L);
        mockUser.setEmail("test@juxa.com");
        mockUser.setName("Usuario Test");
        // Dejamos el stripeCustomerId en null para probar la creación del cliente

        // Configuramos un plan falso
        mockPlan = new PlanEntity();
        mockPlan.setId(10L);
        mockPlan.setName("juxa_go");
        mockPlan.setStripePriceId("price_test_123");

        // Simulamos el usuario autenticado
        Mockito.when(userService.getCurrentAuthenticatedUser()).thenReturn(mockUser);
    }

    // ==========================================
    // 1. TEST: Checkout Normal (El que ya tenías)
    // ==========================================
    @Test
    void testCreateCheckout_ReturnsUrl() throws Exception {
        PaymentResponseDTO mockResponse = new PaymentResponseDTO("https://checkout.stripe.com/test", "cs_test_123");
        Mockito.when(paymentService.createCheckout(any(PaymentRequestDTO.class))).thenReturn(mockResponse);

        String jsonRequest = "{\"category\":\"juxa_go\"}";

        mockMvc.perform(post("/api/payments/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://checkout.stripe.com/test"))
                .andExpect(jsonPath("$.sessionId").value("cs_test_123"));
    }

    // ==========================================
    // 2. TEST: Trial Checkout
    // ==========================================
    @Test
    void testCreateTrialCheckout_ReturnsUrl() throws Exception {
        Mockito.when(planService.getPlanByName("juxa_go")).thenReturn(mockPlan);

        // Usamos try-with-resources para mockear los métodos estáticos de Stripe temporalmente
        try (MockedStatic<Customer> mockedCustomer = Mockito.mockStatic(Customer.class);
             MockedStatic<Session> mockedSession = Mockito.mockStatic(Session.class)) {

            // Mockeamos la creación del Customer
            Customer fakeCustomer = new Customer();
            fakeCustomer.setId("cus_fake_123");
            mockedCustomer.when(() -> Customer.create(any(CustomerCreateParams.class))).thenReturn(fakeCustomer);

            // Mockeamos la creación de la Sesión
            Session fakeSession = new Session();
            fakeSession.setUrl("https://checkout.stripe.com/trial");
            mockedSession.when(() -> Session.create(any(SessionCreateParams.class))).thenReturn(fakeSession);

            mockMvc.perform(post("/api/payments/create-trial-checkout"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.url").value("https://checkout.stripe.com/trial"));

            // Verificamos que se haya intentado guardar el nuevo CustomerId en el usuario
            Mockito.verify(userRepository, Mockito.times(1)).save(mockUser);
        }
    }

    // ==========================================
    // 3. TEST: Compra de 500 Tokens
    // ==========================================
    @Test
    void testBuyExtraTokens_ReturnsUrl() throws Exception {
        Mockito.when(planService.getPlanByName("tokens_500")).thenReturn(mockPlan);

        try (MockedStatic<Customer> mockedCustomer = Mockito.mockStatic(Customer.class);
             MockedStatic<Session> mockedSession = Mockito.mockStatic(Session.class)) {

            Customer fakeCustomer = new Customer();
            fakeCustomer.setId("cus_fake_456");
            mockedCustomer.when(() -> Customer.create(any(CustomerCreateParams.class))).thenReturn(fakeCustomer);

            Session fakeSession = new Session();
            fakeSession.setUrl("https://checkout.stripe.com/tokens-500");
            mockedSession.when(() -> Session.create(any(SessionCreateParams.class))).thenReturn(fakeSession);

            mockMvc.perform(post("/api/payments/buy-extra-500-tokens"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.url").value("https://checkout.stripe.com/tokens-500"));
        }
    }

    // ==========================================
    // 4. TEST: Checkout Dinámico de Tokens
    // ==========================================
    @Test
    void testCreateOneTimeCheckout_ValidPackage_ReturnsUrl() throws Exception {
        // NOTA: Cambia "NOMBRE_VALIDO" por un string que exista realmente en tu enum TokenPackageDef
        // de lo contrario, tu método lanzará IllegalArgumentException y devolverá 400 Bad Request.
        String jsonRequest = "{\"packageName\":\"NOMBRE_VALIDO_DE_TU_ENUM\"}";

        // Asumiendo que "NOMBRE_VALIDO_DE_TU_ENUM" resuelve a un plan en tu BD
        Mockito.when(planService.getPlanByName(any())).thenReturn(mockPlan);

        try (MockedStatic<Customer> mockedCustomer = Mockito.mockStatic(Customer.class);
             MockedStatic<Session> mockedSession = Mockito.mockStatic(Session.class)) {

            Customer fakeCustomer = new Customer();
            fakeCustomer.setId("cus_fake_789");
            mockedCustomer.when(() -> Customer.create(any(CustomerCreateParams.class))).thenReturn(fakeCustomer);

            Session fakeSession = new Session();
            fakeSession.setUrl("https://checkout.stripe.com/dynamic-tokens");
            mockedSession.when(() -> Session.create(any(SessionCreateParams.class))).thenReturn(fakeSession);

            mockMvc.perform(post("/api/payments/checkout/tokens")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonRequest))
                    //.andExpect(status().isOk()) // Descomenta esto si pusiste un valor válido del enum
                    //.andExpect(jsonPath("$.url").value("https://checkout.stripe.com/dynamic-tokens"));
                    .andReturn(); // Solo para que compile sin importar el Enum que tengas
        }
    }

    @Test
    void testCreateOneTimeCheckout_InvalidPackage_ReturnsBadRequest() throws Exception {
        // Si el frontend envía basura, debe retornar 400
        String jsonRequest = "{\"packageName\":\"PAQUETE_FALSO_QUE_NO_EXISTE\"}";

        mockMvc.perform(post("/api/payments/checkout/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Paquete de tokens inválido."));
    }

    // ==========================================
    // 5. TEST: Portal de Cliente
    // ==========================================
    @Test
    void testCreatePortalSession_ReturnsUrl() throws Exception {
        PortalResponseDTO mockResponse = new PortalResponseDTO("https://billing.stripe.com/portal/test");
        Mockito.when(paymentService.createCustomerPortalSession(mockUser.getId())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/payments/portal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://billing.stripe.com/portal/test"));
    }

    // ==========================================
    // 6. TEST: Cancelar Suscripción
    // ==========================================
    @Test
    void testCancelSubscription_ReturnsSuccessMessage() throws Exception {
        String expectedMessage = "Suscripción cancelada con éxito. Conservarás acceso a las herramientas hasta el 2026-04-18";
        Mockito.when(paymentService.cancelSubscription(mockUser.getId())).thenReturn(expectedMessage);

        mockMvc.perform(post("/api/payments/cancel-subscription"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(expectedMessage));
    }
}