package com.juxa.legal_advice.controller;

import com.juxa.legal_advice.config.TokenPackageDef;
import com.juxa.legal_advice.config.exceptions.payment.InvalidStripePayloadException;
import com.juxa.legal_advice.config.exceptions.payment.InvalidWebhookSignatureException;
import com.juxa.legal_advice.config.exceptions.payment.WebhookSyncException;
import com.juxa.legal_advice.dto.PortalResponseDTO;
import com.juxa.legal_advice.dto.UserDataDTO;
import com.juxa.legal_advice.dto.payment.CheckoutRequestDTO;
import com.juxa.legal_advice.dto.payment.PaymentRequestDTO;
import com.juxa.legal_advice.dto.payment.PaymentResponseDTO;
import com.juxa.legal_advice.dto.payment.TokenCheckoutRequestDTO;
import com.juxa.legal_advice.model.PlanEntity;
import com.juxa.legal_advice.service.payment.PaymentService;
import com.juxa.legal_advice.service.payment.StripeWebhookService; // Asegúrate de importar esto
import com.stripe.exception.StripeException;
import com.stripe.param.CustomerCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;///////////////////////////////////////////////////////////
import org.slf4j.LoggerFactory;/////////////////////////////////////
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.juxa.legal_advice.model.UserEntity;
import com.juxa.legal_advice.service.UserService;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.web.bind.annotation.*;

import com.juxa.legal_advice.repository.UserRepository;
import com.juxa.legal_advice.service.PlanService;
import com.stripe.model.Customer;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@Slf4j // <-- Lombok genera el Logger automáticamente
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor // <-- Esto crea el constructor automáticamente para todas las variables 'final'
public class PaymentController {

    private final PaymentService paymentService;
    private final StripeWebhookService stripeWebhookService;
    private final UserService userService;
    private final PlanService planService;
    private final UserRepository userRepository;



    @PostMapping("/create-trial-checkout")
    public ResponseEntity<Map<String, String>> createTrialCheckout() throws StripeException {
        UserEntity user = userService.getCurrentAuthenticatedUser();
        String stripeCustomerId = user.getStripeCustomerId();

        if (stripeCustomerId == null || stripeCustomerId.isEmpty()) {
            CustomerCreateParams customerParams = CustomerCreateParams.builder()
                    .setEmail(user.getEmail())
                    .setName(user.getName())
                    .build();
            Customer customer = Customer.create(customerParams);
            stripeCustomerId = customer.getId();

            user.setStripeCustomerId(stripeCustomerId);
            userRepository.save(user);
            log.info("Customer creado exitosamente en Stripe. customerId={}", stripeCustomerId);
        }

        PlanEntity plan = planService.getPlanByName("juxa_go");

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomer(stripeCustomerId)
                .setSuccessUrl("https://tu-frontend.com/dashboard?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl("https://tu-frontend.com/planes")
                .putMetadata("user_id", String.valueOf(user.getId()))
                .putMetadata("plan_id", String.valueOf(plan.getId()))
                .setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
                        .setTrialPeriodDays(7L)
                        .setTrialSettings(SessionCreateParams.SubscriptionData.TrialSettings.builder()
                                .setEndBehavior(SessionCreateParams.SubscriptionData.TrialSettings.EndBehavior.builder()
                                        .setMissingPaymentMethod(SessionCreateParams.SubscriptionData.TrialSettings.EndBehavior.MissingPaymentMethod.CANCEL)
                                        .build())
                                .build())
                        .putMetadata("user_id", String.valueOf(user.getId()))
                        .putMetadata("plan_id", String.valueOf(plan.getId()))
                        .build())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(plan.getStripePriceId())
                        .setQuantity(1L)
                        .build())
                .setPaymentMethodCollection(SessionCreateParams.PaymentMethodCollection.IF_REQUIRED)
                .build();

        Session session = Session.create(params);
        return ResponseEntity.ok(Map.of("url", session.getUrl()));
    }

    // 👇 AÑADIR ESTE ENDPOINT PARA LA COMPRA DE TOKENS
    @PostMapping("/buy-extra-500-tokens")
    public ResponseEntity<?> buyExtraTokens() {
        try {
            UserEntity user = userService.getCurrentAuthenticatedUser();
            String stripeCustomerId = user.getStripeCustomerId();

            // 1. Si no tiene Customer ID en Stripe, lo creamos
            if (stripeCustomerId == null || stripeCustomerId.isEmpty()) {
                String safeName = (user.getName() != null && !user.getName().trim().isEmpty())
                        ? user.getName()
                        : "Usuario JUXA";

                CustomerCreateParams customerParams = CustomerCreateParams.builder()
                        .setEmail(user.getEmail())
                        .setName(safeName)
                        .build();

                Customer customer = Customer.create(customerParams);
                stripeCustomerId = customer.getId();

                user.setStripeCustomerId(stripeCustomerId);
                userRepository.save(user);
            }

            // 2. Extraer el precio del paquete de tokens desde la BD
            // Asumiendo que guardaste el plan con el nombre 'tokens_500'
            PlanEntity tokenPack = planService.getPlanByName("tokens_500");

            // Definimos la cantidad que otorga este paquete (para que el webhook sepa cuánto sumar)
            String tokensToAdd = "500";

            // 3. Crear la sesión de Checkout (Modo PAYMENT)
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT) // Importante: Es un pago único, no suscripción
                    .setCustomer(stripeCustomerId)
                    .setSuccessUrl("https://tu-frontend.com/dashboard?payment=success")
                    .setCancelUrl("https://tu-frontend.com/planes")
                    // METADATA VITAL PARA EL WEBHOOK
                    .putMetadata("payment_type", "extra_tokens")
                    .putMetadata("user_id", String.valueOf(user.getId()))
                    .putMetadata("token_amount", tokensToAdd)
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setPrice(tokenPack.getStripePriceId())
                            .setQuantity(1L)
                            .build())
                    .build();

            Session session = Session.create(params);

            return ResponseEntity.ok(Map.of("url", session.getUrl()));

        } catch (Exception e) {
            log.error("Error creando checkout de tokens (500). Detalle: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/checkout")
    public ResponseEntity<PaymentResponseDTO> createCheckout(@RequestBody CheckoutRequestDTO request) {

        // 1. Obtenemos al usuario de forma segura desde el token JWT
        UserEntity currentUser = userService.getCurrentAuthenticatedUser();

        // 2. Construimos el UserDataDTO con los datos seguros + la categoría del Frontend
        UserDataDTO userData = new UserDataDTO();
        // Convertimos a String porque tu servicio hace Long.parseLong(user.getUserId())
        userData.setUserId(currentUser.getId().toString());
        userData.setEmail(currentUser.getEmail());
        userData.setCategory(request.getCategory());

        // 3. Empaquetamos todo en tu viejo PaymentRequestDTO
        PaymentRequestDTO paymentRequest = new PaymentRequestDTO();
        paymentRequest.setUserDataDTO(userData);

        // 4. Se lo pasamos a tu servicio ORIGINAL sin modificarle ni una sola línea
        PaymentResponseDTO response = paymentService.createCheckout(paymentRequest);

        return ResponseEntity.ok(response);
    }


    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) throws StripeException {

        // ¡Cero try-catch! Si falla, vuela al GlobalExceptionHandler
        String resultado = stripeWebhookService.handleWebhook(payload, sigHeader);
        return ResponseEntity.ok(resultado);
    }
    @PostMapping("/portal")
    public ResponseEntity<PortalResponseDTO> createPortalSession() {
        // 1. Obtenemos el usuario autenticado desde el Token (vía UserService)
        UserEntity user = userService.getCurrentAuthenticatedUser();

        // 2. Usamos su ID interno para generar la sesión del portal
        PortalResponseDTO response = paymentService.createCustomerPortalSession(user.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cancel-subscription")
    public ResponseEntity<Map<String, String>> cancelSubscription() {
        UserEntity currentUser = userService.getCurrentAuthenticatedUser();

        String resultMessage = paymentService.cancelSubscription(currentUser.getId());

        return ResponseEntity.ok(Map.of("message", resultMessage));
    }

    @PostMapping("/checkout/tokens")
    public ResponseEntity<Map<String, String>> createOneTimeCheckout(@RequestBody TokenCheckoutRequestDTO request) throws StripeException {
        UserEntity user = userService.getCurrentAuthenticatedUser();
        String stripeCustomerId = user.getStripeCustomerId();

        // Si esto falla, lanzará IllegalArgumentException y el GlobalExceptionHandler devolverá 400 Bad Request
        TokenPackageDef selectedPackage = TokenPackageDef.fromDbName(request.getPackageName());

        // 2. Si no tiene Customer ID en Stripe, lo creamos
        if (stripeCustomerId == null || stripeCustomerId.isEmpty()) {
            String safeName = (user.getName() != null && !user.getName().trim().isEmpty()) ? user.getName() : "Usuario JUXA";
            CustomerCreateParams customerParams = CustomerCreateParams.builder()
                    .setEmail(user.getEmail())
                    .setName(safeName)
                    .build();
            Customer customer = Customer.create(customerParams);
            stripeCustomerId = customer.getId();

            user.setStripeCustomerId(stripeCustomerId);
            userRepository.save(user);
        }

        // 3. Extraer el precio del paquete desde la BD usando el nombre del Enum
        // Nota: Debes asegurarte de registrar estos "dbName" en tu tabla de planes/productos
        PlanEntity tokenPack = planService.getPlanByName(selectedPackage.getDbName());

        // 4. Crear la sesión de Checkout
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setCustomer(stripeCustomerId)
                .setSuccessUrl("https://tu-frontend.com/dashboard?payment=success")
                .setCancelUrl("https://tu-frontend.com/planes")
                .putMetadata("payment_type", "extra_tokens")
                .putMetadata("user_id", String.valueOf(user.getId()))
                .putMetadata("token_amount", String.valueOf(selectedPackage.getTokenAmount()))
                .putMetadata("package_name", selectedPackage.getDisplayName())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(tokenPack.getStripePriceId())
                        .setQuantity(1L)
                        .build())
                .build();

        Session session = Session.create(params);
        return ResponseEntity.ok(Map.of("url", session.getUrl()));
    }
}