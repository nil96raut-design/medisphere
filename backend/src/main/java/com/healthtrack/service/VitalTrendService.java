package com.healthtrack.service;

import com.healthtrack.dto.NurseDtos.*;
import com.healthtrack.entity.*;
import com.healthtrack.event.EventConstants;
import com.healthtrack.event.EventPublisher;
import com.healthtrack.repository.VitalRecordRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class VitalTrendService {

    private static final Logger log = LoggerFactory.getLogger(VitalTrendService.class);

    private final VitalRecordRepository vitalRecordRepository;
    private final EventPublisher eventPublisher;

    private final Map<Long, List<VitalRecord>> vitalTrendCache = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public VitalTrendResponse getTrend(Long patientId) {
        List<VitalRecord> recent = vitalRecordRepository
                .findByPatientIdOrderByRecordedAtDesc(patientId);
        if (recent.size() > 5) recent = recent.subList(0, 5);

        int abnormalCount = 0;
        for (VitalRecord v : recent) {
            if (Boolean.TRUE.equals(v.getAlertFlag())) abnormalCount++;
        }

        boolean consecutiveAbnormal = abnormalCount >= 3;
        String trend = analyzeTrend(recent);

        return new VitalTrendResponse(
                patientId,
                recent.isEmpty() ? "" : recent.get(0).getPatient().getFirstName() + " " + recent.get(0).getPatient().getLastName(),
                recent.stream().map(this::mapVitalResponse).toList(),
                consecutiveAbnormal,
                abnormalCount,
                trend);
    }

    @Transactional(readOnly = true)
    public List<VitalRecord> getLastNVitals(Long patientId, int n) {
        List<VitalRecord> records = vitalRecordRepository
                .findByPatientIdOrderByRecordedAtDesc(patientId);
        return records.size() > n ? records.subList(0, n) : records;
    }

    public boolean checkConsecutiveAbnormal(Long patientId) {
        List<VitalRecord> recent = vitalTrendCache.getOrDefault(patientId, List.of());
        if (recent.size() < 3) return false;
        long abnormalCount = recent.stream()
                .filter(v -> Boolean.TRUE.equals(v.getAlertFlag()))
                .limit(3)
                .count();
        return abnormalCount >= 3;
    }

    @CacheEvict(value = "vitalTrend", key = "#patientId")
    public void updateCache(Long patientId, VitalRecord record) {
        vitalTrendCache.compute(patientId, (key, list) -> {
            List<VitalRecord> updated = list == null ? new ArrayList<>() : new ArrayList<>(list);
            updated.add(0, record);
            if (updated.size() > 3) updated = updated.subList(0, 3);
            return updated;
        });

        if (checkConsecutiveAbnormal(patientId)) {
            eventPublisher.publish(EventConstants.CRITICAL_VITAL_ESCALATED, 0L,
                    Map.of("patientId", patientId,
                            "consecutiveAbnormalCount",
                            vitalTrendCache.getOrDefault(patientId, List.of()).stream()
                                    .filter(v -> Boolean.TRUE.equals(v.getAlertFlag())).count(),
                            "reason", "3 consecutive abnormal vitals"));
        }
    }

    private String analyzeTrend(List<VitalRecord> records) {
        if (records.size() < 2) return "INSUFFICIENT_DATA";
        boolean stable = records.stream().allMatch(v -> !Boolean.TRUE.equals(v.getAlertFlag()));
        if (stable) return "STABLE";
        boolean improving = records.size() >= 3 &&
                records.get(0).getAlertFlag() == null &&
                records.get(records.size() - 1).getAlertFlag() != null;
        if (improving) return "IMPROVING";
        boolean deteriorating = records.size() >= 3 &&
                records.get(0).getAlertFlag() != null &&
                records.get(records.size() - 1).getAlertFlag() == null;
        if (deteriorating) return "DETERIORATING";
        return "UNSTABLE";
    }

    private VitalRecordResponse mapVitalResponse(VitalRecord v) {
        return new VitalRecordResponse(
                v.getId(), v.getPatient().getId(),
                v.getPatient().getFirstName() + " " + v.getPatient().getLastName(),
                v.getNurse().getId(), v.getNurse().getFullName(),
                v.getBloodPressure(), v.getHeartRate(), v.getTemperature(),
                v.getSpo2(), v.getSugarLevel(),
                v.getAlertFlag(), v.getAlertReason(), v.getRecordedAt());
    }
}
