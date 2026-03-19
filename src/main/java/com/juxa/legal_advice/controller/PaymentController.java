package com.juxa.legal_advice.controller;

import com.juxa.legal_advice.dto.PortalRequestDTO;
import com.juxa.legal_advice.dto.PortalResponseDTO;
import com.juxa.legal_advice.dto.payment.PaymentRequestDTO;
import com.juxa.legal_advice.dto.payment.PaymentResponseDTO;
import com.juxa.legal_advice.service.payment.PaymentService;
import com.juxa.legal_advice.service.payment.StripeWebhookService; // Asegúrate de importar esto
import org.slf4j.Logger;///////////////////////////////////////////////////////////
import org.slf4j.LoggerFactory;/////////////////////////////////////
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private static final Logger log = LoggerFactory.getLogger(PaymentController.class); ////////////////
    @Autowired
    private PaymentService paymentService;

    @Autowired
    private StripeWebhookService stripeWebhookService;

    @PostMapping("/checkout")
    public ResponseEntity<PaymentResponseDTO> createCheckout(@RequestBody PaymentRequestDTO request){
        return ResponseEntity.ok(paymentService.createCheckout(request));
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
}