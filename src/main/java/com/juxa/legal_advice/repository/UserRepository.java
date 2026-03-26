package com.juxa.legal_advice.repository;

import com.juxa.legal_advice.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);

    // Assuming the UserEntity has a field named 'subscriptionPlan'
    // and SubscriptionEntity has a field named 'currentPeriodEnd'
    @Modifying
    @Query("UPDATE UserEntity u SET u.subscriptionPlan = 'FREE' WHERE u.id IN " +
            "(SELECT s.user.id FROM SubscriptionEntity s WHERE s.currentPeriodEnd < CURRENT_TIMESTAMP) " +
            "AND u.subscriptionPlan != 'FREE'")
    int updateExpiredSubscriptionsToFree();
}



