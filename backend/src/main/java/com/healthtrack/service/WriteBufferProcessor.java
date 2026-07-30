package com.healthtrack.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthtrack.dto.BillingDtos.SettleRequest;
import com.healthtrack.dto.NurseDtos.VitalRecordRequest;
import com.healthtrack.entity.User;
import com.healthtrack.entity.WriteBufferEntry;
import com.healthtrack.repository.UserRepository;
import com.healthtrack.repository.WriteBufferRepository;
import com.healthtrack.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class WriteBufferProcessor {

    private static final Logger log = LoggerFactory.getLogger(WriteBufferProcessor.class);
    private final WriteBufferRepository writeBufferRepository;
    private final ObjectMapper objectMapper;
    private final NurseService nurseService;
    private final BillingService billingService;
    private final UserRepository userRepository;
    private final io.micrometer.core.instrument.MeterRegistry meterRegistry;

    @Scheduled(fixedDelay = 60000) // Run every 60s
    @Transactional
    public void processBuffer() {
        log.debug("Checking write buffer for pending operations...");
        List<WriteBufferEntry> pendingEntries = writeBufferRepository.findTop50ByStatusOrderByPriorityAscCreatedAtAsc("PENDING");
        
        if (pendingEntries.isEmpty()) {
            return;
        }

        for (WriteBufferEntry entry : pendingEntries) {
            try {
                // SLA Check
                Duration processingTime = Duration.between(entry.getCreatedAt(), OffsetDateTime.now());
                if (processingTime.toMinutes() >= 5) {
                    meterRegistry.counter("sla.breach.count", "system", "write_buffer", "operation", entry.getOperationType()).increment();
                    log.error("SLA Breach detected for WriteBuffer entry {} (Operation: {}). Elapsed: {} mins", 
                              entry.getId(), entry.getOperationType(), processingTime.toMinutes());
                }

                String operationType = entry.getOperationType();
                Map<String, Object> payloadMap = entry.getPayload();

                log.info("Processing buffered operation: {} with priority {}", operationType, entry.getPriority());

                if ("RECORD_VITALS".equals(operationType)) {
                    VitalRecordRequest request = objectMapper.convertValue(payloadMap.get("request"), VitalRecordRequest.class);
                    Long userId = Long.valueOf(payloadMap.get("userId").toString());
                    
                    User user = userRepository.findById(userId).orElse(null);
                    if (user != null) {
                        nurseService.recordVitals(request, new UserPrincipal(user));
                    }
                } else if ("BILL_SETTLE".equals(operationType)) {
                    SettleRequest request = objectMapper.convertValue(payloadMap.get("request"), SettleRequest.class);
                    Long userId = Long.valueOf(payloadMap.get("userId").toString());
                    
                    User user = userRepository.findById(userId).orElse(null);
                    if (user != null) {
                        billingService.settle(request, new UserPrincipal(user));
                    }
                }
                
                entry.setStatus("COMPLETED");
                entry.setProcessedAt(OffsetDateTime.now());
                writeBufferRepository.save(entry);
                meterRegistry.counter("write_buffer.processed.success", "operation", operationType).increment();
            } catch (Exception e) {
                log.error("Failed to process buffered entry {}, incrementing retry count. Error: {}", entry.getId(), e.getMessage(), e);
                meterRegistry.counter("write_buffer.processed.failed").increment();
                
                entry.setRetryCount(entry.getRetryCount() + 1);
                entry.setErrorMessage(e.getMessage());
                if (entry.getRetryCount() >= 5) {
                    entry.setStatus("DEAD_LETTER");
                    log.error("WriteBuffer entry {} reached DEAD_LETTER state.", entry.getId());
                }
                writeBufferRepository.save(entry);
            }
        }
    }
}
