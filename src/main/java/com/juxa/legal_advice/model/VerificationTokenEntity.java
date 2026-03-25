package com.juxa.legal_advice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@Table(name = "verification_tokens")
public class VerificationTokenEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String token;

    @OneToOne(targetEntity = UserEntity.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "user_id")
    private UserEntity user;

    private LocalDateTime expiryDate;

    public VerificationTokenEntity(String token, UserEntity user) {
        this.token = token;
        this.user = user;
        // Por ejemplo, el token expira en 24 horas
        this.expiryDate = LocalDateTime.now().plusHours(24);
    }
}