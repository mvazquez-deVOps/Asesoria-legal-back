package com.juxa.legal_advice.repository;

import com.juxa.legal_advice.model.DeletedAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeletedAccountRepository extends JpaRepository<DeletedAccountEntity, Long> {
    boolean existsByEmail(String email);
}