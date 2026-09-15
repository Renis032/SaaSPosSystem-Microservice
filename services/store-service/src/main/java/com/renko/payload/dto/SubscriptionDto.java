package com.renko.payload.dto;

import com.renko.domain.SubscriptionPlan;
import com.renko.domain.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionDto
{
    private Long id;
    private Long storeId;
    private SubscriptionPlan plan;
    private SubscriptionStatus status;
    private String stripeCustomerId;
    private String stripeSubscriptionId;
    private LocalDateTime trialEndsAt;
    private LocalDateTime currentPeriodEnd;
    private boolean entitled;
}
