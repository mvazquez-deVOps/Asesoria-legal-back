package com.juxa.legal_advice.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "plan_usage")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanUsageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // MapsId hace que el ID de esta entidad pueda ser el mismo que el del usuario
    // si así lo prefieres, o simplemente mantiene la relación 1 a 1 limpia.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    @Builder.Default
    @Column(name = "queries_used_today")
    private Integer queriesUsedToday = 0;

    @Builder.Default
    @Column(name = "files_uploaded_today")
    private Integer filesUploadedToday = 0;

    @Column(name = "last_reset_date")
    private LocalDate lastResetDate;

    @PrePersist
    protected void onCreate() {
        if (this.lastResetDate == null) {
            this.lastResetDate = LocalDate.now();
        }
    }
}