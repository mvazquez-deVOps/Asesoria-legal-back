package com.juxa.legal_advice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PortalResponseDTO {
    // Aquí devolveremos la URL segura generada por Stripe
    private String url;
}