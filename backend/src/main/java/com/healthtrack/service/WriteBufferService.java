package com.healthtrack.service;

import com.healthtrack.repository.WriteBufferRepository;
import com.healthtrack.entity.WriteBufferEntry;
import com.healthtrack.entity.QueuePriority;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class WriteBufferService {

    private static final Logger log = LoggerFactory.getLogger(WriteBufferService.class);
    private final WriteBufferRepository writeBufferRepository;
    private final ObjectMapper objectMapper;
    private final io.micrometer.core.instrument.MeterRegistry meterRegistry;
    
    private static final int MAX_QUEUE_SIZE = 5000;

    public void queueFailedWrite(String operationType, Object payload, QueuePriority priority) {
        try {
            long currentQueueSize = writeBufferRepository.countByStatus("PENDING");
            
            if (currentQueueSize >= MAX_QUEUE_SIZE && priority != QueuePriority.CRITICAL) {
                log.warn("Write buffer max capacity reached ({}), rejecting {} priority operation: {}", 
                         MAX_QUEUE_SIZE, priority, operationType);
                meterRegistry.counter("write_buffer.rejected.count", "operation", operationType).increment();
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "System under heavy load. Please try again later.");
            }

            Map<String, Object> payloadMap;
            if (payload instanceof Map) {
                payloadMap = (Map<String, Object>) payload;
            } else {
                payloadMap = objectMapper.convertValue(payload, new TypeReference<Map<String, Object>>() {});
            }

            WriteBufferEntry entry = WriteBufferEntry.builder()
                    .operationType(operationType)
                    .payload(payloadMap)
                    .priority(priority)
                    .build();

            writeBufferRepository.save(entry);
            meterRegistry.counter("write_buffer.queued.count", "operation", operationType, "priority", priority.name()).increment();
            log.info("Queued failed write {} to PostgreSQL buffer with priority {}", operationType, priority);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to queue write to buffer: {}", e.getMessage(), e);
        }
    }
}
