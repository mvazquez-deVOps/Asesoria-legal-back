package com.juxa.legal_advice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthResponseDTO {
    private String token;
    private String userId;
    private String email;
    private String name;
    private Integer loginCount;
    private String role;
    private String subscriptionPlan;
    private String personType; // Se añade para que el Front sepa si ya eligió
}