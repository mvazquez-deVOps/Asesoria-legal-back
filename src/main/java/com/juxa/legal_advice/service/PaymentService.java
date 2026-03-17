package com.juxa.legal_advice.service;

import com.juxa.legal_advice.dto.PaymentRequestDTO;
import com.juxa.legal_advice.dto.PaymentResponseDTO;
import com.juxa.legal_advice.dto.UserDataDTO;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

@Service
public class PaymentService {

    @Value("${STRIPE_KEY}")
    private String stripeKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeKey;
    }

    public PaymentResponseDTO createCheckout(PaymentRequestDTO request) {
        UserDataDTO user = request.getUserData();

        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(user.getAmountInCents()) // Usa el método que creamos con BigDecimal
                    .setCurrency("mxn")
                    .setReceiptEmail(user.getEmail())
                    .putMetadata("user_id", user.getUserId())
                    .putMetadata("category", user.getCategory())
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            // Retornamos el DTO con el secret que React necesita
            return new PaymentResponseDTO(intent.getClientSecret(), intent.getId());

        } catch (StripeException e) {
            // Manejo de errores específicos de Stripe
            throw new RuntimeException("Error en la API de Stripe: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Error inesperado: " + e.getMessage());
        }
    }
}