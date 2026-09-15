package com.renko.payload.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreReportDto
{
    private Long storeId;
    private long orderCount;
    private double grossSales;
    private double refundTotal;
    private double netSales;
    private long refundCount;
    private long lowStockCount;
    private long openShiftCount;
}
