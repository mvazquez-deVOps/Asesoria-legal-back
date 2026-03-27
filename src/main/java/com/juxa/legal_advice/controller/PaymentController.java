package com.juxa.legal_advice.controller;

import com.juxa.legal_advice.dto.PortalRequestDTO;
import com.juxa.legal_advice.dto.PortalResponseDTO;
import com.juxa.legal_advice.dto.UserDataDTO;
import com.juxa.legal_advice.dto.payment.CheckoutRequestDTO;
import com.juxa.legal_advice.dto.payment.PaymentRequestDTO;
import com.juxa.legal_advice.dto.payment.PaymentResponseDTO;
import com.juxa.legal_advice.model.PlanEntity;
import com.juxa.legal_advice.service.payment.PaymentService;
import com.juxa.legal_advice.service.payment.StripeWebhookService; // Asegúrate de importar esto
import com.stripe.param.CustomerCreateParams;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.juxa.legal_advice.model.UserEntity;
import com.juxa.legal_advice.repository.UserRepository;
import com.juxa.legal_advice.service.PlanService;
import com.juxa.legal_advice.service.UserService;
import com.stripe.model.Customer;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor // <-- Esto crea el constructor automáticamente para todas las variables 'final'
public class PaymentController {
    private static final Logger log = LoggerFactory.getLogger(PaymentController.class); ////////////////

    private final PaymentService paymentService;
    private final StripeWebhookService stripeWebhookService;
    private final UserService userService;
    private final PlanService planService;
    private final UserRepository userRepository;



    @PostMapping("/create-trial-checkout")
    public ResponseEntity<?> createTrialCheckout() {
        try {
            UserEntity user = userService.getCurrentAuthenticatedUser();
            String stripeCustomerId = user.getStripeCustomerId();
            /// ////////////////////////////////////////
            // BLOQUEO DE SEGURIDAD: Evitar que alguien que ya tiene un plan activo vuelva a pedir un Trial
          /*  if (user.getSubscriptionPlan() != null || user.getSubscriptionPlan() != "FREE") {
                return ResponseEntity.status(400).body(Map.of("error", "El usuario ya tiene un plan activo."));
      } */ /// //////////////// DEBERIAMOS refactorizar que la  propiedad de subscripción no sea FREE automaticamente
            // 1. CREACIÓN DE CLIENTE SEGURA
            if (stripeCustomerId == null || stripeCustomerId.isEmpty()) {

                String name = user.getName();

                CustomerCreateParams customerParams = CustomerCreateParams.builder()
                        .setEmail(user.getEmail())
                        .setName(name)
                        .build();

                Customer customer = Customer.create(customerParams);
                stripeCustomerId = customer.getId();

                // Guardamos el nuevo ID en MySQL
                user.setStripeCustomerId(stripeCustomerId);
                userRepository.save(user);
            }

            // 2. EXTRAER PLAN DE LA BDd
            // Nota: Escribe el nombre exactamente como está en la columna 'name' de tu tabla plans
            PlanEntity plan = planService.getPlanByName("juxa_go");

            // 3. CONSTRUIR SESIÓN CON METADATA PARA EL WEBHOOK
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomer(stripeCustomerId)
                    .setSuccessUrl("https://tu-frontend.com/dashboard?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl("https://tu-frontend.com/planes")
                    .putMetadata("user_id", String.valueOf(user.getId()))
                    .putMetadata("plan_id", String.valueOf(plan.getId()))
                    .setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
                            .setTrialPeriodDays(7L)
                            // 👇 NUEVO: Le decimos a Stripe qué hacer si acaba el trial y no hay tarjeta (CANCELAR)
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
                    // 👇 CAMBIO CLAVE: Cambiar ALWAYS por IF_REQUIRED (o puedes borrar esta línea, ya que es el default)
                    .setPaymentMethodCollection(SessionCreateParams.PaymentMethodCollection.IF_REQUIRED)
                    .build();

            Session session = Session.create(params);

            return ResponseEntity.ok(Map.of("url", session.getUrl()));

        } catch (Exception e) {
            System.err.println("--- [ERROR CREANDO CHECKOUT] --- " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // 👇 AÑADIR ESTE ENDPOINT PARA LA COMPRA DE TOKENS
    @PostMapping("/buy-extra-500-tokens")
    public ResponseEntity<?> buyExtraTokens() {
        log.error("sad");
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
            log.error("Error creando checkout de tokens: {}", e.getMessage());
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
            @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            // Guardamos el mensaje que nos regresa el servicio
            String resultado = stripeWebhookService.handleWebhook(payload, sigHeader);

            // Retornamos un HTTP 200 OK con el mensaje en el body
            return ResponseEntity.ok(resultado);

        } catch (Exception e) {
            System.err.println("Error procesando el webhook: " + e.getMessage());
            // Si algo falla, retornamos un HTTP 400 Bad Request
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error en webhook: " + e.getMessage());
        }
    }

    @PostMapping("/portal")
    public ResponseEntity<?> createPortalSession(@RequestBody PortalRequestDTO request) {
        try {
            PortalResponseDTO response = paymentService.createCustomerPortalSession(request.getUserId());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error creando el portal de cliente: {}", e.getMessage());
            // Si el usuario no tiene Customer ID u ocurre otro error, devolvemos un 400
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/cancel-subscription")
    public ResponseEntity<?> cancelSubscription() {
        try {
            // Obtiene el usuario directamente del JWT.
            UserEntity currentUser = userService.getCurrentAuthenticatedUser();

            String resultMessage = paymentService.cancelSubscription(currentUser.getId());

            // Devuelve un JSON para el Front
            return ResponseEntity.ok(Map.of("message", resultMessage));

        } catch (RuntimeException e) {
            // Si la suscripción ya estaba cancelada o no se encontró, devolvemos un error 400
            log.error("Error al cancelar la suscripción del usuario: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            // Fallback para errores de conectividad con Stripe o servidor
            log.error("Error inesperado al cancelar la suscripción", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ocurrió un error inesperado al conectar con el servidor de pagos."));
        }
    }
}