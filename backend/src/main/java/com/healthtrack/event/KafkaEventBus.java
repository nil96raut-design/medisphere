package com.healthtrack.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.event.mode", havingValue = "kafka")
public class KafkaEventBus implements EventBus {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventBus.class);
    private static final String TOPIC_PREFIX = "hms.";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(String eventType, Long hospitalId, Object payload) {
        send(toTopic(eventType), eventType, hospitalId, payload);
    }

    @Override
    @Async
    public void publishAsync(String eventType, Long hospitalId, Object payload) {
        send(toTopic(eventType), eventType, hospitalId, payload);
    }

    private void send(String topic, String eventType, Long hospitalId, Object payload) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("eventType", eventType);
            message.put("hospitalId", hospitalId);
            message.put("payload", payload);
            message.put("timestamp", System.currentTimeMillis());

            kafkaTemplate.send(topic, String.valueOf(hospitalId), message)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish event {} to topic {}: {}", eventType, topic, ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("Failed to serialize/publish event {}: {}", eventType, e.getMessage());
        }
    }

    private String toTopic(String eventType) {
        return TOPIC_PREFIX + eventType.toLowerCase().replace('_', '.');
    }
}
