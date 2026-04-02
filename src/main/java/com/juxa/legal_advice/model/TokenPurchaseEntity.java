package com.juxa.legal_advice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class TokenPurchaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "plan_usage_id")
    private PlanUsageEntity planUsage;

    private int remainingAmount;    // Lo que le queda a ESTA bolsa
    private LocalDateTime expiresAt; // Fecha de compra + 1 año

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}