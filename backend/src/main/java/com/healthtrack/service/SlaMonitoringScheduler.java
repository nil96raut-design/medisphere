package com.healthtrack.service;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SlaMonitoringScheduler {

    private static final Logger log = LoggerFactory.getLogger(SlaMonitoringScheduler.class);

    private final JdbcTemplate jdbcTemplate;
    private final EmergencyAlertService emergencyAlertService;
    private final MeterRegistry meterRegistry;

    @Scheduled(fixedRate = 60000) // Run every 60 seconds
    @Transactional(readOnly = true)
    public void monitorLabAndQueueSlas() {
        OffsetDateTime threshold = OffsetDateTime.now().minusMinutes(60);

        // 1. Lab Orders pending over 60 minutes (Database-agnostic query parameter)
        List<Map<String, Object>> delayedLabs = jdbcTemplate.queryForList("""
            SELECT id, hospital_id, patient_id, test_name, created_at
            FROM lab_test_order
            WHERE status IN ('ORDERED', 'SAMPLE_COLLECTED')
              AND created_at < ?
            LIMIT 20
            """, threshold);

        for (Map<String, Object> lab : delayedLabs) {
            Long hospitalId = ((Number) lab.get("hospital_id")).longValue();
            Long patientId = ((Number) lab.get("patient_id")).longValue();
            String testName = (String) lab.get("test_name");

            meterRegistry.counter("sla.breach.count", "system", "lab", "test", testName).increment();
            log.warn("SLA BREACH: Lab Order ID {} for test '{}' (Patient ID {}) exceeded 60m SLA window",
                    lab.get("id"), testName, patientId);

            emergencyAlertService.triggerEmergencyAlert(
                    hospitalId, patientId, "SLA_BREACH_LAB",
                    "Lab Order '" + testName + "' exceeded 60m turnaround SLA"
            );
        }
    }
}
