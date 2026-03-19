package com.juxa.legal_advice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDTO {

    private String url;        // La URL a la que React/Angular debe redirigir al usuario
    private String sessionId;  // El ID de la sesión de Stripe (cs_test_...) por si lo necesitas

}