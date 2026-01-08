package com.juxa.legal_advice.model;

import jakarta.persistence.*;
import lombok.*;
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

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Builder.Default // Esto soluciona el Warning del Builder
    @Column(name = "login_count")
    private Integer loginCount = 0;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.loginCount == null) this.loginCount = 0;
    }
}