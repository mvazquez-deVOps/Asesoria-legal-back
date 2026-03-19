package com.juxa.legal_advice.model;

import jakarta.persistence.*;
import lombok.*;

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

    // Relación 1 a 1 con el usuario: previene N+1 y bucles infinitos de memoria
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private UserEntity user;

    @Builder.Default
    @Column(name = "daily_queries_count", nullable = false)
    private Integer dailyQueriesCount = 0;

    @Builder.Default
    @Column(name = "daily_files_count", nullable = false)
    private Integer dailyFilesCount = 0;
}