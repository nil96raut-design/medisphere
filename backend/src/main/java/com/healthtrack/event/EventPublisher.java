package com.healthtrack.event;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthtrack.entity.EventOutbox;
import com.healthtrack.repository.EventOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final EventOutboxRepository eventOutboxRepository;
    private final ObjectMapper objectMapper;

    public void publish(String eventType, Long hospitalId, Object payload) {
        saveToOutbox(eventType, hospitalId, payload);
    }

    public void publishAsync(String eventType, Long hospitalId, Object payload) {
        saveToOutbox(eventType, hospitalId, payload);
    }

    private void saveToOutbox(String eventType, Long hospitalId, Object payload) {
        Map<String, Object> payloadMap;
        try {
            if (payload instanceof Map) {
                payloadMap = (Map<String, Object>) payload;
            } else {
                payloadMap = objectMapper.convertValue(payload, new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            payloadMap = Map.of("data", payload.toString());
        }

        String aggregateId = payloadMap.containsKey("id") ? payloadMap.get("id").toString() : 
                             payloadMap.containsKey("patientId") ? payloadMap.get("patientId").toString() : 
                             UUID.randomUUID().toString();

        EventOutbox outbox = EventOutbox.builder()
                .aggregateId(aggregateId)
                .aggregateType("HOSPITAL_" + hospitalId)
                .eventType(eventType)
                .payload(payloadMap)
                .build();
        eventOutboxRepository.save(outbox);
    }
}
