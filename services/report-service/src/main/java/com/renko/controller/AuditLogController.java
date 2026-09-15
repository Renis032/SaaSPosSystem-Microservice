package com.renko.controller;

import com.renko.entities.AuditLogEntity;
import com.renko.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/audit-logs")
public class AuditLogController
{
    private final AuditLogService auditLogService;

    @PostMapping
    public ResponseEntity<Void> ingest(@RequestBody Map<String, Object> body)
    {
        Long storeId = body.get("storeId") != null ? Long.valueOf(String.valueOf(body.get("storeId"))) : null;
        String action = String.valueOf(body.getOrDefault("action", "UNKNOWN"));
        String entityType = String.valueOf(body.getOrDefault("entityType", "Unknown"));
        String entityId = body.get("entityId") != null ? String.valueOf(body.get("entityId")) : null;
        String details = body.get("details") != null ? String.valueOf(body.get("details")) : null;
        String before = body.get("beforeState") != null ? String.valueOf(body.get("beforeState")) : null;
        String after = body.get("afterState") != null ? String.valueOf(body.get("afterState")) : null;
        auditLogService.recordChange(storeId, action, entityType, entityId, before, after, details);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/store/{storeId}")
    public ResponseEntity<List<AuditLogEntity>> list(@PathVariable Long storeId) throws Exception
    {
        return ResponseEntity.ok(auditLogService.listForStore(storeId));
    }
}
