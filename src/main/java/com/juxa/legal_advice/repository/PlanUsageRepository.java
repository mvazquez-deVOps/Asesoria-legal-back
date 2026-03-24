package com.juxa.legal_advice.repository;

import com.juxa.legal_advice.model.PlanUsageEntity;
import com.juxa.legal_advice.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlanUsageRepository extends JpaRepository<PlanUsageEntity, Long> {


    Optional<PlanUsageEntity> findByUserId(Long userId);
    Optional<PlanUsageEntity> findByUser(UserEntity user);
}