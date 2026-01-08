package com.juxa.legal_advice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponseDTO {
    private String token;
    private String userId;
    private String email;
    private String name;
    private Integer loginCount;
    private String role;
    private String subscriptionPlan; // Asegúrate de que este campo exista aquí
}