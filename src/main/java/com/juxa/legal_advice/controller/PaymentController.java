package com.juxa.legal_advice.controller;


import com.juxa.legal_advice.dto.PaymentResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    @PostMapping("/checkout")
    public ResponseEntity<PaymentResponseDTO> createCheckout(@RequestBody PaymentRequestDTO request){
        return ResponseEntity.ok(paymentService.createCheckout(request))
    }

}
