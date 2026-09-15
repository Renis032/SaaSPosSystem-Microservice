package com.renko.service.impl;

import com.renko.client.CatalogServiceClient;
import com.renko.client.SalesServiceClient;
import com.renko.payload.dto.OrderDto;
import com.renko.payload.dto.RefundDto;
import com.renko.payload.dto.ShiftReportDto;
import com.renko.payload.dto.StoreReportDto;
import com.renko.service.ReportService;
import com.renko.service.StoreAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService
{
    private final StoreAccessService storeAccessService;
    private final SalesServiceClient salesServiceClient;
    private final CatalogServiceClient catalogServiceClient;

    @Override
    public StoreReportDto getStoreReport(Long storeId) throws Exception
    {
        storeAccessService.requireStoreAccess(storeId);

        List<OrderDto> orders = salesServiceClient.ordersByStore(storeId);
        List<RefundDto> refunds = salesServiceClient.refundsByStore(storeId);
        List<ShiftReportDto> shifts = salesServiceClient.shiftsByStore(storeId);

        double grossSales = orders.stream().mapToDouble(o -> o.getTotalAmount() != null ? o.getTotalAmount() : 0.0).sum();
        double refundTotal = refunds.stream().mapToDouble(r -> r.getAmount() != null ? r.getAmount() : 0.0).sum();
        long openShifts = shifts.stream().filter(s -> s.getShiftEnd() == null).count();
        long lowStock = catalogServiceClient.lowStock(storeId).size();

        return StoreReportDto.builder()
                .storeId(storeId)
                .orderCount(orders.size())
                .grossSales(grossSales)
                .refundTotal(refundTotal)
                .netSales(grossSales - refundTotal)
                .refundCount(refunds.size())
                .lowStockCount(lowStock)
                .openShiftCount(openShifts)
                .build();
    }
}
