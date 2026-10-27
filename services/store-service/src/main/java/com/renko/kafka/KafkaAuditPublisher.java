package com.renko.kafka;

import tools.jackson.databind.ObjectMapper;
import com.renko.event.AuditEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaAuditPublisher
{
    private final ObjectProvider<KafkaTemplate<String, String>> kafkaTemplates;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final String topic;

    public KafkaAuditPublisher(ObjectProvider<KafkaTemplate<String, String>> kafkaTemplates,
                               ObjectMapper objectMapper,
                               @Value("${app.kafka.enabled:true}") boolean enabled,
                               @Value("${app.kafka.topics.audit:renko.audit}") String topic)
    {
        this.kafkaTemplates = kafkaTemplates;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.topic = topic;
    }

    public boolean publish(AuditEvent event)
    {
        if(!enabled)
        {
            return false;
        }
        KafkaTemplate<String, String> kafka = kafkaTemplates.getIfAvailable();
        if(kafka == null)
        {
            return false;
        }
        try
        {
            String key = event.getStoreId() != null ? String.valueOf(event.getStoreId()) : "none";
            String json = objectMapper.writeValueAsString(event);
            kafka.send(topic, key, json).whenComplete((result, ex) ->
            {
                if(ex != null)
                {
                    log.warn("Kafka audit publish failed action={} entity={}: {}",
                            event.getAction(), event.getEntityType(), ex.getMessage());
                }
            });
            return true;
        }
        catch(Exception e)
        {
            log.warn("Kafka audit publish failed action={} entity={}: {}",
                    event.getAction(), event.getEntityType(), e.getMessage());
            return false;
        }
    }
}
