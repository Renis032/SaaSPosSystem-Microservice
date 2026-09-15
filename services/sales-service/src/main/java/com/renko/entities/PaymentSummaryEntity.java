package com.renko.entities;

import com.renko.domain.PaymentType;
import lombok.Data;

@Data
public class PaymentSummaryEntity
{
    private PaymentType type;
    private Double totalAmount;
    private int transactionCount;
    private double percentage;
}
