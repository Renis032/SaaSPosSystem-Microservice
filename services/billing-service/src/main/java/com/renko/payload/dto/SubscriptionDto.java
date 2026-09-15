package com.renko.payload.dto;

import com.renko.domain.SubscriptionPlan;
import com.renko.domain.SubscriptionStatus;
import com.renko.entities.StoreSubscriptionEntity;
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

    public static SubscriptionDto from(StoreSubscriptionEntity entity)
    {
        return SubscriptionDto.builder()
                .id(entity.getId())
                .storeId(entity.getStoreId())
                .plan(entity.getPlan())
                .status(entity.getStatus())
                .stripeCustomerId(entity.getStripeCustomerId())
                .stripeSubscriptionId(entity.getStripeSubscriptionId())
                .trialEndsAt(entity.getTrialEndsAt())
                .currentPeriodEnd(entity.getCurrentPeriodEnd())
                .entitled(entity.isEntitled())
                .build();
    }
}
