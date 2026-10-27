package com.renko.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent
{
    private Long storeId;
    private Long actorUserId;
    private String actorEmail;
    private String action;
    private String entityType;
    private String entityId;
    private String details;
    private String beforeState;
    private String afterState;
    private String source;
    private Instant occurredAt;
}
