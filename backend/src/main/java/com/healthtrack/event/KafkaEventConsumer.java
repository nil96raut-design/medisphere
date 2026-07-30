package com.healthtrack.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.event.mode", havingValue = "kafka")
public class KafkaEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventConsumer.class);

    private final ApplicationEventPublisher localPublisher;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = {
            "hms.appointment.created",
            "hms.bill.generated",
            "hms.lab.result.ready",
            "hms.prescription.dispensed",
            "hms.patient.admitted",
            "hms.patient.discharged",
            "hms.insurance.claim.submitted"
    })
    public void onEvent(Map<String, Object> message) {
        try {
            String eventType = (String) message.get("eventType");
            Number hospitalIdNum = (Number) message.get("hospitalId");
            Object payload = message.get("payload");

            if (eventType == null || hospitalIdNum == null) return;

            Long hospitalId = hospitalIdNum.longValue();
            localPublisher.publishEvent(new HmsEvent(this, eventType, hospitalId, payload));
        } catch (Exception e) {
            log.error("Failed to process Kafka event: {}", e.getMessage());
        }
    }
}
