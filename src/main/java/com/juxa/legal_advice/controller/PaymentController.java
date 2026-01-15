package com.juxa.legal_advice.controller;


import com.juxa.legal_advice.dto.PaymentRequestDTO;
import com.juxa.legal_advice.dto.PaymentResponseDTO;
import com.juxa.legal_advice.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "https://back-legaladvice-284685729356.us-central1.run.app")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/checkout")
    public ResponseEntity<PaymentResponseDTO> createCheckout(@RequestBody PaymentRequestDTO request){
        return ResponseEntity.ok(paymentService.createCheckout(request));
    }

}
