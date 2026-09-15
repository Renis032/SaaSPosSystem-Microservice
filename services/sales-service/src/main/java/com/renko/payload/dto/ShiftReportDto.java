package com.renko.payload.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiftReportDto
{
    private Long id;

    private LocalDateTime shiftStart;
    private LocalDateTime shiftEnd;

    private Double totalSales;
    private Double totalRefunds;
    private Double netSales;
    private Double totalOrders;

    private Long cashierId;
    private Long storeId;
    private Long branchId;

    private List<PaymentSummaryDto> paymentSummaries;
    private List<Long> topSellingProductIds;
    private List<Long> recentOrderIds;
    private List<Long> refundIds;
}
