package com.juxa.legal_advice.repository;

import com.juxa.legal_advice.model.ProcessedStripeEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedStripeEventRepository extends JpaRepository<ProcessedStripeEventEntity, String> {
    // No necesitas agregar nada aquí.
    // JpaRepository ya incluye el método existsById(String id) que usaremos.
}