package com.juxa.legal_advice.controller;

import com.juxa.legal_advice.dto.PaymentRequestDTO;
import com.juxa.legal_advice.dto.PaymentResponseDTO;
import com.juxa.legal_advice.service.PaymentService;
import com.juxa.legal_advice.service.payment.StripeWebhookService; // Asegúrate de importar esto
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private StripeWebhookService stripeWebhookService;

    @PostMapping("/checkout")
    public ResponseEntity<PaymentResponseDTO> createCheckout(@RequestBody PaymentRequestDTO request){
        return ResponseEntity.ok(paymentService.createCheckout(request));
    }

    // NUEVO: Endpoint para el Webhook de Stripe
    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        try {
            stripeWebhookService.handleWebhook(payload, sigHeader);
            return ResponseEntity.ok("Webhook recibido");
        } catch (Exception e) {
            System.err.println("Error procesando el webhook: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error en webhook");
        }
    }
}