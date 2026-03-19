package com.juxa.legal_advice.service.payment;

import com.juxa.legal_advice.dto.PortalResponseDTO;
import com.juxa.legal_advice.dto.payment.PaymentRequestDTO;
import com.juxa.legal_advice.dto.payment.PaymentResponseDTO;
import com.juxa.legal_advice.dto.UserDataDTO;
import com.juxa.legal_advice.model.PlanEntity;
import com.juxa.legal_advice.model.UserEntity;
import com.juxa.legal_advice.repository.PlanRepository;
import com.juxa.legal_advice.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

@Service
public class PaymentService {

    @Value("${stripe.api.key}")
    private String stripeKey;

    // Aquí va la URL de tu frontend (React/Angular) para redirigir al usuario
    @Value("${frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlanRepository planRepository;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeKey;
    }

    public PaymentResponseDTO createCheckout(PaymentRequestDTO request) {
        UserDataDTO user = request.getUserData();

        try {

            // 1. Buscamos el plan en la base de datos según lo que pidió el usuario (ej. "estudiantes")
            PlanEntity plan = planRepository.findByName(user.getCategory())
                    .orElseThrow(() -> new RuntimeException("Plan no encontrado en la base de datos"));

            String stripePriceId = plan.getStripePriceId();

            // 2. Creamos los parámetros para la Sesión de Checkout (Modo Suscripción)
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setSuccessUrl(frontendUrl + "/pago-exitoso?session_id={CHECKOUT_SESSION_ID}")  /// ///CAMBIAR
                    .setCancelUrl(frontendUrl + "/pago-cancelado")  ///  //// CAMBIAR
                    .setCustomerEmail(user.getEmail())
                    // PASO CLAVE: Guardamos en Stripe los IDs de nuestra base de datos
                    // para saber de quién es la suscripción cuando nos llegue el Webhook
                    .setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
                            .putMetadata("user_id", String.valueOf(user.getUserId()))
                            .putMetadata("plan_id", String.valueOf(plan.getId()))
                            .build())
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPrice(stripePriceId)
                                    .setQuantity(1L)
                                    .build()
                    )
                    .build();

            // 3. Generamos la sesión en Stripe
            Session session = Session.create(params);

            // 4. Retornamos la URL al frontend.
            // NOTA: Asegúrate de adaptar tu PaymentResponseDTO para recibir una URL (String)
            return new PaymentResponseDTO(session.getUrl(), session.getId());

        } catch (StripeException e) {
            // Le pasamos la 'e' al final para no perder el rastro exacto del error
            throw new RuntimeException("Error en la API de Stripe: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Error inesperado: " + e.getMessage(), e);
        }
    }

    public PortalResponseDTO createCustomerPortalSession(Long userId) {
        // 1. Buscamos al usuario en la base de datos
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado en la base de datos"));

        // 2. Verificamos que el usuario tenga un ID de cliente de Stripe
        String customerId = user.getStripeCustomerId();
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new RuntimeException("El usuario no tiene un método de pago registrado en Stripe. No puede acceder al portal.");
        }

        try {
            // 3. Creamos los parámetros para el portal
            com.stripe.param.billingportal.SessionCreateParams params =
                    com.stripe.param.billingportal.SessionCreateParams.builder()
                            .setCustomer(customerId)
                            // URL a la que el usuario regresará al hacer clic en "Volver" en el portal de Stripe
                            .setReturnUrl(frontendUrl + "/mi-cuenta") // <--- CAMBIAR según la ruta de tu frontend
                            .build();

            // 4. Creamos la sesión en Stripe
            com.stripe.model.billingportal.Session portalSession =
                    com.stripe.model.billingportal.Session.create(params);

            // 5. Devolvemos la URL generada
            return new PortalResponseDTO(portalSession.getUrl());

        } catch (StripeException e) {
            throw new RuntimeException("Error en la API de Stripe al generar el portal: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Error inesperado al generar el portal: " + e.getMessage(), e);
        }
    }
}