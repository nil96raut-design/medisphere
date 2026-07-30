package com.healthtrack.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class DataRetentionScheduler {

    private static final Logger log = LoggerFactory.getLogger(DataRetentionScheduler.class);

    private final JdbcTemplate jdbcTemplate;

    // Purge records soft-deleted over 7 years ago (HIPAA 7-year retention limit)
    @Scheduled(cron = "0 0 3 * * SUN") // Every Sunday at 3 AM
    @Transactional
    public void purgeExpiredSoftDeletedRecords() {
        OffsetDateTime retentionCutoff = OffsetDateTime.now().minusYears(7);
        log.info("Starting data retention cleanup for records soft-deleted before {}", retentionCutoff);

        try {
            int deletedPatients = jdbcTemplate.update("DELETE FROM patients WHERE deleted_at IS NOT NULL AND deleted_at < ?", retentionCutoff);
            int deletedRecords = jdbcTemplate.update("DELETE FROM medical_record WHERE deleted_at IS NOT NULL AND deleted_at < ?", retentionCutoff);
            int deletedVitals = jdbcTemplate.update("DELETE FROM vital_record WHERE deleted_at IS NOT NULL AND deleted_at < ?", retentionCutoff);
            int deletedLabs = jdbcTemplate.update("DELETE FROM lab_test_order WHERE deleted_at IS NOT NULL AND deleted_at < ?", retentionCutoff);

            log.info("Retention cleanup completed. Purged: {} patients, {} medical records, {} vitals, {} lab orders",
                    deletedPatients, deletedRecords, deletedVitals, deletedLabs);
        } catch (Exception e) {
            log.error("Failed to execute data retention purge: {}", e.getMessage(), e);
        }
    }
}
