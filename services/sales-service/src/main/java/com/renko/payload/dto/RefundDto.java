package com.renko.payload.dto;

import com.renko.domain.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundDto
{
    private Long id;

    private Long orderId;

    private String reason;

    private Double amount;

    private Long shiftReportId;

    private Long cashierId;
    private String cashierName;

    private Long storeId;

    private PaymentType paymentType;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
