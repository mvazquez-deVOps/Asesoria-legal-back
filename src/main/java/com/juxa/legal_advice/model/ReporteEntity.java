package com.juxa.legal_advice.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reportes_incidencias")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteEntity {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private UserEntity usuario;

    private String nivel;
    private String categoria;
    private String nombreIncidencia; //Referente al incidente y no al usuario

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    private String email;
    private String plataforma;
    private String fechaHoraCliente;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate(){
        this.createdAt = LocalDateTime.now();
    }
}
