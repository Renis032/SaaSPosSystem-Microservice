package com.renko.payload.dto;

import com.renko.domain.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSummaryDto
{
    private PaymentType type;
    private Double totalAmount;
    private int transactionCount;
    private double percentage;
}
