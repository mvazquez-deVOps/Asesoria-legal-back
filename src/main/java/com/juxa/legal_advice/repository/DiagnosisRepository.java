package com.juxa.legal_advice.repository;

import com.juxa.legal_advice.model.DiagnosisEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiagnosisRepository extends JpaRepository<DiagnosisEntity, Long> {

    /** * Buscar diagnósticos por userId
     * (Este es el método principal de búsqueda en la v1.0.1)
     */
    List<DiagnosisEntity> findByUserId(String userId);

    /** * Obtener el último diagnóstico del usuario para continuar la charla
     */
    Optional<DiagnosisEntity> findFirstByUserIdOrderByCreatedAtDesc(String userId);

    // El método findByEmail se eliminó porque la columna 'email'
    // ya no existe en la tabla 'diagnoses'.
}