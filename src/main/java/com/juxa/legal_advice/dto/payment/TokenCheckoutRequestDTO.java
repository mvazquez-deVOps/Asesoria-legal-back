package com.juxa.legal_advice.dto.payment;

import lombok.Data;

@Data
public class TokenCheckoutRequestDTO {
    // Aquí el frontend enviará algo como "bolsa_estratega"
    private String packageName;
}