package com.juxa.legal_advice.repository;

import com.juxa.legal_advice.model.ReporteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReporteRepository extends JpaRepository<ReporteEntity, Long> {
}
