package com.juxa.legal_advice.model;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "diagnoses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiagnosisEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
    @Column(length = 2000)
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

    // Metadatos
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

}

