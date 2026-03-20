package com.juxa.legal_advice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSubscriptionResponseDTO {
    private boolean hasActiveSubscription;
    private String planName; // Aquí mandaremos lo que dice "subscription_plan"
    private String status;
    private LocalDateTime currentPeriodEnd;
    private boolean willCancelAtPeriodEnd;
}