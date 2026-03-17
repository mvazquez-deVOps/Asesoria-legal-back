package com.juxa.legal_advice.service.payment;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripeWebhookService {

    // Este secreto lo generaremos en el Paso 3 con Stripe CLI
    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    public void handleWebhook(String payload, String sigHeader) {
        Event event;

        try {
            // 1. Verificamos que la petición realmente venga de Stripe
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            // Firma inválida (alguien intentó hackear tu endpoint)
            throw new RuntimeException("Firma de Stripe inválida");
        }

        // 2. Manejamos el evento según su tipo
        switch (event.getType()) {
            case "checkout.session.completed":
                System.out.println("¡Éxito! Un usuario ha completado el pago.");
                // Aquí en el futuro llamaremos a tu DB para guardar la suscripción
                break;
            case "invoice.paid":
                System.out.println("¡Éxito! Se ha cobrado un mes de suscripción.");
                break;
            case "invoice.payment_failed":
                System.out.println("El cobro automático de la suscripción falló.");
                break;
            default:
                System.out.println("Evento no manejado por ahora: " + event.getType());
        }
    }
}