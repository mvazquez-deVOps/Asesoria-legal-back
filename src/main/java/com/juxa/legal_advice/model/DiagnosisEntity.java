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

    private String userId;   //  requerido por findByUser
    private String name;
    private String email;
    private String phone;
    private String category;
    private String subcategory;
    @Column(length = 2000)
    private String description;

    private String amount;
    private String location;
    private String counterparty;
    private String processStatus;

    private Boolean hasChildren;
    private Boolean hasViolence;
    private String diagnosisPreference;


    private String folio;    // requerido por mapToDTO
    private LocalDateTime createdAt;

}