package com.juxa.legal_advice.repository;

import com.juxa.legal_advice.model.SubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, Long> {
    Optional<SubscriptionEntity> findByStripeSubscriptionId(String stripeSubscriptionId);
    Optional<SubscriptionEntity> findByUserId(Long userId);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE subscriptions SET status = 'inactive' " +
            "WHERE current_period_end < DATE_SUB(NOW(), INTERVAL 7 DAY) AND status != 'inactive'",
            nativeQuery = true)
    int updateExpiredSubscriptionsStatus();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE subscriptions SET status = 'inactive' " +
            "WHERE current_period_end < NOW() " +
            "AND status = 'trialing'",
            nativeQuery = true)
    int updateExpiredTrialingSubscriptionsStatus();
}