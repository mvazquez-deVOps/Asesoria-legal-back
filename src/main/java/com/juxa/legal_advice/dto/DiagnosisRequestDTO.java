package com.juxa.legal_advice.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiagnosisRequestDTO {

    // Identificación del usuario
    @NotBlank
    private String name;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    @Size(min = 10, max = 15)
    private String phone;

    // Contexto jurídico
    @NotBlank
    private String category;

    @NotBlank
    private String subcategory;

    @NotBlank
    @Size(max = 2000)
    private String description;

    // Variables de éxito
    @DecimalMin("0.0")
    private Double amount;

    @NotBlank
    private String location;

    @NotBlank
    private String counterparty;

    @NotBlank
    private String processStatus;
}

