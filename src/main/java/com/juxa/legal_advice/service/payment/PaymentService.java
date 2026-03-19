package com.juxa.legal_advice.service.payment;

import com.juxa.legal_advice.dto.PaymentRequestDTO;
import com.juxa.legal_advice.dto.PaymentResponseDTO;
import com.juxa.legal_advice.dto.UserDataDTO;
import com.juxa.legal_advice.model.PlanEntity;
import com.juxa.legal_advice.repository.PlanRepository;
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

            // OJO: En tu BD actual los stripe_price_id dicen "estudiantes", etc.
            // Para que Stripe funcione, debes ir a tu panel de Stripe, crear los productos,
            // copiar sus IDs reales (empiezan con price_...) y actualizar tu tabla plans.
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
}