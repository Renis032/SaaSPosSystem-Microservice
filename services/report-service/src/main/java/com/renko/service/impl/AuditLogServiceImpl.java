package com.renko.service.impl;

import com.renko.entities.AuditLogEntity;
import com.renko.event.AuditEvent;
import com.renko.payload.dto.UserDto;
import com.renko.repository.AuditLogRepository;
import com.renko.service.AuditLogService;
import com.renko.service.StoreAccessService;
import com.renko.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogServiceImpl implements AuditLogService
{
    private final AuditLogRepository auditLogRepository;
    private final UserService userService;
    private final StoreAccessService storeAccessService;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long storeId, String action, String entityType, String entityId, String details)
    {
        recordChange(storeId, action, entityType, entityId, null, null, details);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordChange(Long storeId, String action, String entityType, String entityId,
                             String beforeState, String afterState, String details)
    {
        try
        {
            Long actorId = null;
            String actorEmail = null;
            try
            {
                UserDto user = userService.getCurrentUser();
                actorId = user.getId();
                actorEmail = user.getEmail();
            }
            catch(Exception ignored) {}

            persist(storeId, actorId, actorEmail, action, entityType, entityId, details, beforeState, afterState, null);
        }
        catch(Exception e)
        {
            log.warn("Failed to write audit log action={} entityType={}: {}", action, entityType, e.getMessage());
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ingest(AuditEvent event)
    {
        if(event == null || event.getAction() == null || event.getAction().isBlank())
        {
            return;
        }
        try
        {
            LocalDateTime occurredAt = event.getOccurredAt() != null
                    ? LocalDateTime.ofInstant(event.getOccurredAt(), ZoneOffset.UTC)
                    : null;
            persist(event.getStoreId(), event.getActorUserId(), event.getActorEmail(),
                    event.getAction(), event.getEntityType(), event.getEntityId(),
                    event.getDetails(), event.getBeforeState(), event.getAfterState(), occurredAt);
        }
        catch(Exception e)
        {
            log.warn("Failed to ingest audit event action={} entityType={}: {}",
                    event.getAction(), event.getEntityType(), e.getMessage());
        }
    }

    private void persist(Long storeId, Long actorId, String actorEmail, String action, String entityType,
                         String entityId, String details, String beforeState, String afterState,
                         LocalDateTime createdAt)
    {
        AuditLogEntity.AuditLogEntityBuilder builder = AuditLogEntity.builder()
                .storeId(storeId)
                .actorUserId(actorId)
                .actorEmail(actorEmail)
                .action(action)
                .entityType(entityType != null ? entityType : "Unknown")
                .entityId(entityId)
                .details(details)
                .beforeState(beforeState)
                .afterState(afterState);
        if(createdAt != null)
        {
            builder.createdAt(createdAt);
        }
        auditLogRepository.save(builder.build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogEntity> listForStore(Long storeId) throws Exception
    {
        storeAccessService.requireStoreAccess(storeId);
        return auditLogRepository.findTop100ByStoreIdOrderByCreatedAtDesc(storeId);
    }
}
