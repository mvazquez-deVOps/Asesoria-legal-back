package com.juxa.legal_advice.model;

import jakarta.persistence.*;
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

    @Column(name = "user_id")
    private String userId;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String history;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "plan")
    private SubscriptionPlan plan = SubscriptionPlan.SINGLE_DIAGNOSIS;

    @Builder.Default
    private boolean isPaid = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}