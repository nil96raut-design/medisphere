package com.healthtrack.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthtrack.entity.ClinicalRecordHistory;
import com.healthtrack.repository.ClinicalRecordHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ClinicalHistoryService {

    private static final Logger log = LoggerFactory.getLogger(ClinicalHistoryService.class);

    private final ClinicalRecordHistoryRepository historyRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void recordVersion(String recordType, Long recordId, Object payload, String modifiedBy, String changeReason) {
        try {
            int nextVersion = historyRepository.findTopByRecordTypeAndRecordIdOrderByVersionNumberDesc(recordType, recordId)
                    .map(h -> h.getVersionNumber() + 1)
                    .orElse(1);

            Map<String, Object> payloadMap = objectMapper.convertValue(payload, new TypeReference<Map<String, Object>>() {});

            ClinicalRecordHistory history = ClinicalRecordHistory.builder()
                    .recordType(recordType)
                    .recordId(recordId)
                    .versionNumber(nextVersion)
                    .snapshotPayload(payloadMap)
                    .modifiedBy(modifiedBy)
                    .changeReason(changeReason != null ? changeReason : "Record updated")
                    .build();

            historyRepository.save(history);
            log.info("Saved version {} for {} ID {}", nextVersion, recordType, recordId);
        } catch (Exception e) {
            log.error("Failed to save clinical history version: {}", e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<ClinicalRecordHistory> getHistory(String recordType, Long recordId) {
        return historyRepository.findByRecordTypeAndRecordIdOrderByVersionNumberDesc(recordType, recordId);
    }
}
