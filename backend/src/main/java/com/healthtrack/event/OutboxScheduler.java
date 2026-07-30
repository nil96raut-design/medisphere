package com.healthtrack.event;

import com.healthtrack.entity.EventOutbox;
import com.healthtrack.repository.EventOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OutboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxScheduler.class);

    private final EventOutboxRepository eventOutboxRepository;
    private final EventBus eventBus;
    private final MeterRegistry meterRegistry;

    @Scheduled(fixedDelayString = "${outbox.scheduler.delay:5000}")
    @Transactional
    public void processOutbox() {
        List<EventOutbox> pendingEvents = eventOutboxRepository.findPendingEvents();
        if (pendingEvents.isEmpty()) {
            return;
        }

        for (EventOutbox event : pendingEvents) {
            try {
                Long hospitalId = Long.parseLong(event.getAggregateType().replace("HOSPITAL_", ""));
                
                if (event.getRetryCount() > 0) {
                    long backoffTime = (long) Math.pow(2, event.getRetryCount()) * 1000L;
                    long timeSinceCreationMillis = OffsetDateTime.now().toInstant().toEpochMilli() - event.getCreatedAt().toInstant().toEpochMilli();
                    if (timeSinceCreationMillis < backoffTime) {
                        continue; // Skip until backoff expires
                    }
                }

                // SLA Check
                java.time.Duration processingTime = java.time.Duration.between(event.getCreatedAt(), OffsetDateTime.now());
                if (processingTime.toMinutes() >= 5) {
                    meterRegistry.counter("sla.breach.count", "system", "outbox", "operation", event.getEventType()).increment();
                    log.error("SLA Breach detected for Outbox event {} (Type: {}). Elapsed: {} mins", 
                              event.getId(), event.getEventType(), processingTime.toMinutes());
                }

                eventBus.publish(event.getEventType(), hospitalId, event.getPayload());

                event.setStatus("PROCESSED");
                event.setProcessedAt(OffsetDateTime.now());
                eventOutboxRepository.save(event);
                meterRegistry.counter("outbox.event.processed", "type", event.getEventType()).increment();
            } catch (Exception e) {
                log.error("Failed to process event {}", event.getId(), e);
                event.setRetryCount(event.getRetryCount() + 1);
                if (event.getRetryCount() >= 5) {
                    event.setStatus("DEAD_LETTER");
                    log.error("Event {} moved to DEAD_LETTER queue after 5 retries", event.getId());
                    meterRegistry.counter("outbox.event.dlq", "type", event.getEventType()).increment();
                } else {
                    meterRegistry.counter("outbox.event.failed", "type", event.getEventType()).increment();
                }
                eventOutboxRepository.save(event);
            }
        }
    }
}
