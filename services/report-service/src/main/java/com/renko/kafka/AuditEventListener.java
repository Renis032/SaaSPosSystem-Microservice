package com.renko.kafka;

import tools.jackson.databind.ObjectMapper;
import com.renko.event.AuditEvent;
import com.renko.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class AuditEventListener
{
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${app.kafka.topics.audit:renko.audit}", groupId = "${spring.application.name:report-service}")
    public void onMessage(String payload)
    {
        try
        {
            AuditEvent event = objectMapper.readValue(payload, AuditEvent.class);
            auditLogService.ingest(event);
        }
        catch(Exception e)
        {
            log.warn("Failed to consume audit event: {}", e.getMessage());
        }
    }
}
