package com.juxa.legal_advice.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "deleted_accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeletedAccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        this.deletedAt = LocalDateTime.now();
    }
}