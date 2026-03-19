package com.juxa.legal_advice.repository;

import com.juxa.legal_advice.model.PaymentHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentHistoryRepository extends JpaRepository<PaymentHistoryEntity, Long> {

    // Útil para verificar si una factura específica (in_...) ya fue registrada
    Optional<PaymentHistoryEntity> findByStripeInvoiceId(String stripeInvoiceId);

    // Útil para traer todo el historial de pagos de una suscripción en particular
    List<PaymentHistoryEntity> findBySubscriptionId(Long subscriptionId);
}