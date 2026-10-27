package com.renko.kafka;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.renko.event.AuditEvent;
import com.renko.service.AuditLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditEventListenerTest
{
    @Mock
    private AuditLogService auditLogService;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    void ingestParsesPayload()
    {
        AuditEventListener listener = new AuditEventListener(auditLogService, objectMapper);
        listener.onMessage("{\"storeId\":1,\"action\":\"ORDER_CREATE\",\"entityType\":\"Order\",\"entityId\":\"50\"}");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditLogService).ingest(captor.capture());
        assertEquals("ORDER_CREATE", captor.getValue().getAction());
        assertEquals(1L, captor.getValue().getStoreId());
        assertEquals("50", captor.getValue().getEntityId());
    }
}
