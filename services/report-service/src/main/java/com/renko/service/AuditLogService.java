package com.renko.service;

import com.renko.entities.AuditLogEntity;
import com.renko.event.AuditEvent;

import java.util.List;

public interface AuditLogService
{
    void record(Long storeId, String action, String entityType, String entityId, String details);

    void recordChange(Long storeId,
                      String action,
                      String entityType,
                      String entityId,
                      String beforeState,
                      String afterState,
                      String details);

    void ingest(AuditEvent event);

    List<AuditLogEntity> listForStore(Long storeId) throws Exception;
}
