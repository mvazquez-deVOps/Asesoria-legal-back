package com.juxa.legal_advice.repository;

import com.juxa.legal_advice.model.DiagnosisEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiagnosisRepository extends JpaRepository<DiagnosisEntity, Long> {

    /** Buscar diagnósticos por email */
    List<DiagnosisEntity> findByEmail(String email);

    /** Buscar diagnósticos por userId */
    List<DiagnosisEntity> findByUserId(String userId);

    Optional<DiagnosisEntity> findFirstByUserIdOrderByCreatedAtDesc(String userId);
}
