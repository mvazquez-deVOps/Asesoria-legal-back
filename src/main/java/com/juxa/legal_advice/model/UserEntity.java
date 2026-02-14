package com.juxa.legal_advice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private String name;

    private String role;

    @Column(name = "phone")
    private String phone;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Builder.Default // Esto soluciona el Warning del Builder
    @Column(name = "login_count")
    private Integer loginCount = 0;

    @Builder.Default
    @Column(name = "subscription_plan")
    private String subscriptionPlan = "FREE"; // Valor por defecto

    @Column(name = "person_type")
    private String personType;

    @Column(name = "trial_end_date")
    private LocalDateTime trialEndDate;

    @Column(name = "daily_message_count")
    private Integer dailyMessageCount = 0;

    @Column(name = "last_message_date")
    private LocalDate lastMessageDate;

    @PrePersist
    protected void onCreate() {
        // 1. Tiempos base
        this.createdAt = LocalDateTime.now();

        // 2. Inicialización de contadores
        if (this.loginCount == null) this.loginCount = 0;
        this.dailyMessageCount = 0;
        this.lastMessageDate = LocalDate.now();

        // 3. Lógica de Negocio: 60 días de prueba (Trial)
        this.trialEndDate = LocalDateTime.now().plusDays(60);
    }
    }



