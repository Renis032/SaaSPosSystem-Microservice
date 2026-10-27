package com.renko.service.impl;

import com.renko.client.AuthHeaderForwarder;
import com.renko.event.AuditEvent;
import com.renko.kafka.KafkaAuditPublisher;
import com.renko.payload.dto.UserDto;
import com.renko.service.AuditLogService;
import com.renko.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogServiceImpl implements AuditLogService
{
    @Qualifier("reportRestClient")
    private final RestClient reportRestClient;
    private final AuthHeaderForwarder authHeaderForwarder;
    private final KafkaAuditPublisher kafkaAuditPublisher;
    private final UserService userService;

    @Value("${spring.application.name:catalog-service}")
    private String source;

    @Override
    public void record(Long storeId, String action, String entityType, String entityId, String details)
    {
        recordChange(storeId, action, entityType, entityId, null, null, details);
    }

    @Override
    public void recordChange(Long storeId, String action, String entityType, String entityId,
                             String beforeState, String afterState, String details)
    {
        AuditEvent event = AuditEvent.builder()
                .storeId(storeId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .beforeState(beforeState)
                .afterState(afterState)
                .source(source)
                .occurredAt(Instant.now())
                .build();
        try
        {
            UserDto user = userService.getCurrentUser();
            event.setActorUserId(user.getId());
            event.setActorEmail(user.getEmail());
        }
        catch(Exception ignored) {}

        if(kafkaAuditPublisher.publish(event))
        {
            return;
        }
        forwardHttp(event);
    }

    private void forwardHttp(AuditEvent event)
    {
        try
        {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("storeId", event.getStoreId());
            body.put("action", event.getAction() != null ? event.getAction() : "UNKNOWN");
            body.put("entityType", event.getEntityType() != null ? event.getEntityType() : "Unknown");
            body.put("entityId", event.getEntityId() != null ? event.getEntityId() : "");
            body.put("details", event.getDetails() != null ? event.getDetails() : "");
            body.put("beforeState", event.getBeforeState() != null ? event.getBeforeState() : "");
            body.put("afterState", event.getAfterState() != null ? event.getAfterState() : "");
            body.put("actorUserId", event.getActorUserId());
            body.put("actorEmail", event.getActorEmail());
            var req = reportRestClient.post()
                    .uri("/api/audit-logs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body);
            String auth = authHeaderForwarder.currentAuthorizationHeader();
            if(auth != null) req = req.header(HttpHeaders.AUTHORIZATION, auth);
            req.retrieve().toBodilessEntity();
        }
        catch(Exception e)
        {
            log.warn("Failed to forward audit log: {}", e.getMessage());
        }
    }

    @Override
    public List<?> listForStore(Long storeId) throws Exception
    {
        return Collections.emptyList();
    }
}
