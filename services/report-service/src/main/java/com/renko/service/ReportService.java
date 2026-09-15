package com.renko.service;

import com.renko.payload.dto.StoreReportDto;

public interface ReportService
{
    StoreReportDto getStoreReport(Long storeId) throws Exception;
}
