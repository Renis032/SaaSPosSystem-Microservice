package com.renko.controller;

import com.renko.entities.AuditLogEntity;
import com.renko.payload.dto.StoreReportDto;
import com.renko.service.AuditLogService;
import com.renko.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reports")
public class ReportController
{
    private final ReportService reportService;
    private final AuditLogService auditLogService;

    @GetMapping("/store/{storeId}")
    @PreAuthorize("hasAnyRole('OWNER','STORE_MANAGER','BRANCH_MANAGER','ADMIN')")
    public ResponseEntity<StoreReportDto> storeReport(@PathVariable Long storeId) throws Exception
    {
        return ResponseEntity.ok(reportService.getStoreReport(storeId));
    }

    @GetMapping("/store/{storeId}/audit")
    @PreAuthorize("hasAnyRole('OWNER','STORE_MANAGER','ADMIN')")
    public ResponseEntity<List<AuditLogEntity>> auditLog(@PathVariable Long storeId) throws Exception
    {
        return ResponseEntity.ok(auditLogService.listForStore(storeId));
    }
}
