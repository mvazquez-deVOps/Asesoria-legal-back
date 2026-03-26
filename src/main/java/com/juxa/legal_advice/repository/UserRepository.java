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
    @Query(value = "UPDATE users SET subscription_plan = 'FREE' WHERE id IN " +
            "(SELECT user_id FROM subscriptions WHERE current_period_end < DATE_SUB(NOW(), INTERVAL 7 DAY)) " +
            "AND subscription_plan != 'FREE'", nativeQuery = true)
    int updateExpiredSubscriptionsToFree();
}



