package com.healthtrack.service;

import com.healthtrack.repository.EventOutboxRepository;
import com.healthtrack.repository.WriteBufferRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResourceMonitoringService {

    private static final Logger log = LoggerFactory.getLogger(ResourceMonitoringService.class);

    private final WriteBufferRepository writeBufferRepository;
    private final EventOutboxRepository eventOutboxRepository;
    private final MeterRegistry meterRegistry;

    @PostConstruct
    public void initGauges() {
        Gauge.builder("system.write_buffer.pending.count", writeBufferRepository, repo -> repo.countByStatus("PENDING"))
                .description("Number of pending items in the PostgreSQL Write Buffer")
                .register(meterRegistry);

        Gauge.builder("system.event_outbox.pending.count", eventOutboxRepository, repo -> repo.findPendingEvents().size())
                .description("Number of pending events in the Outbox queue")
                .register(meterRegistry);
    }

    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void monitorResourcesAndAlert() {
        long writeBufferPending = writeBufferRepository.countByStatus("PENDING");
        long outboxPending = eventOutboxRepository.findPendingEvents().size();

        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        double memoryUsagePercent = ((double) usedMemory / totalMemory) * 100;

        log.debug("Resource Monitor - Memory Usage: {}MB ({:.1f}%), WriteBuffer Pending: {}, Outbox Pending: {}",
                usedMemory / (1024 * 1024), memoryUsagePercent, writeBufferPending, outboxPending);

        // Alert Threshold Checks
        if (memoryUsagePercent > 85.0) {
            log.warn("RESOURCE ALERT: High JVM Memory Usage detected: {:.1f}% used (Total: {}MB)",
                    memoryUsagePercent, totalMemory / (1024 * 1024));
            meterRegistry.counter("system.resource.alert.memory").increment();
        }

        if (writeBufferPending > 3000) {
            log.warn("RESOURCE ALERT: High WriteBuffer Queue backlog detected: {} pending entries", writeBufferPending);
            meterRegistry.counter("system.resource.alert.write_buffer").increment();
        }

        if (outboxPending > 1000) {
            log.warn("RESOURCE ALERT: High EventOutbox Queue backlog detected: {} pending events", outboxPending);
            meterRegistry.counter("system.resource.alert.outbox").increment();
        }
    }
}
