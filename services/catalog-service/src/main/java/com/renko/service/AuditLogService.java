package com.renko.service;

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

    List<?> listForStore(Long storeId) throws Exception;
}
