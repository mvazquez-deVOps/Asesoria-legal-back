package com.juxa.legal_advice.dto.payment;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CheckoutRequestDTO {
    // El frontend solo manda: {"category": "estudiantes"}
    private String category;
}