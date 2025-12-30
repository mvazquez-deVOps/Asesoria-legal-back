package com.juxa.legal_advice.repository;
import com.juxa.legal_advice.model.DiagnosisEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface DiagnosisRepository extends JpaRepository<DiagnosisEntity, Long> {
    // Puedes agregar métodos personalizados si lo necesitas, por ejemplo:
    // List<DiagnosisEntity> findByEmail(String email);
}
