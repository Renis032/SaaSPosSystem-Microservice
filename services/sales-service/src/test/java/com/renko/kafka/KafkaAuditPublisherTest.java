package com.renko.kafka;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.renko.event.AuditEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaAuditPublisherTest
{
    @Mock
    private ObjectProvider<KafkaTemplate<String, String>> kafkaTemplates;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    void disabledSkipsBroker()
    {
        KafkaAuditPublisher publisher = new KafkaAuditPublisher(kafkaTemplates, objectMapper, false, "renko.audit");
        assertFalse(publisher.publish(AuditEvent.builder().action("ORDER_CREATE").build()));
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void enabledPublishesJsonKeyedByStore()
    {
        when(kafkaTemplates.getIfAvailable()).thenReturn(kafkaTemplate);
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));
        KafkaAuditPublisher publisher = new KafkaAuditPublisher(kafkaTemplates, objectMapper, true, "renko.audit");

        assertTrue(publisher.publish(AuditEvent.builder()
                .storeId(1L)
                .action("ORDER_CREATE")
                .entityType("Order")
                .entityId("50")
                .build()));

        verify(kafkaTemplate).send(eq("renko.audit"), eq("1"), contains("ORDER_CREATE"));
    }
}
