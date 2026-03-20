package com.juxa.legal_advice.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "processed_stripe_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedStripeEventEntity {

    // Usamos String porque los IDs de Stripe son del tipo "evt_1PXYZ..."
    @Id
    @Column(name = "id", nullable = false, unique = true, length = 255)
    private String id;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}