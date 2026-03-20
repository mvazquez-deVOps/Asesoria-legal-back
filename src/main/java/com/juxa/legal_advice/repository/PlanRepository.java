package com.juxa.legal_advice.repository;

import com.juxa.legal_advice.model.PlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlanRepository extends JpaRepository<PlanEntity, Long> {
    Optional<PlanEntity> findByName(String name);
    Optional<PlanEntity> findByStripePriceId(String stripePriceId);
}