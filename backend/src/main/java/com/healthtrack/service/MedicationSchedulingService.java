package com.healthtrack.service;

import com.healthtrack.dto.ClinicalSafetyDtos.MedicationScheduleResponse;
import com.healthtrack.entity.*;
import com.healthtrack.event.EventConstants;
import com.healthtrack.event.EventPublisher;
import com.healthtrack.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MedicationSchedulingService {

    private static final Logger log = LoggerFactory.getLogger(MedicationSchedulingService.class);

    private final MedicationScheduleRepository medicationScheduleRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final PatientRepository patientRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public List<MedicationScheduleResponse> generateSchedules(Long prescriptionItemId) {
        PrescriptionItem item = prescriptionItemRepository.findById(prescriptionItemId)
                .orElseThrow(() -> new IllegalArgumentException("Prescription item not found: " + prescriptionItemId));

        Long patientId = item.getMedicalRecord().getPatient().getId();
        String freq = item.getFrequency().toLowerCase().trim();
        int dosesPerDay = parseFrequency(freq);
        if (dosesPerDay <= 0) dosesPerDay = 1;

        List<MedicationSchedule> schedules = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now();
        int durationDays = parseDuration(item.getDuration());

        List<OffsetDateTime> dailyTimes = generateDailyTimes(dosesPerDay, now);

        for (int day = 0; day < durationDays; day++) {
            for (OffsetDateTime baseTime : dailyTimes) {
                OffsetDateTime scheduledTime = baseTime.plusDays(day);
                if (scheduledTime.isBefore(now)) continue;

                if (!medicationScheduleRepository.existsByPrescriptionItemIdAndScheduledTime(
                        prescriptionItemId, scheduledTime)) {
                    MedicationSchedule schedule = MedicationSchedule.builder()
                            .prescriptionItemId(prescriptionItemId)
                            .patientId(patientId)
                            .scheduledTime(scheduledTime)
                            .build();
                    schedules.add(medicationScheduleRepository.save(schedule));
                }
            }
        }

        eventPublisher.publish(EventConstants.MEDICATION_SCHEDULED,
                item.getHospital().getId(),
                Map.of("prescriptionItemId", prescriptionItemId, "patientId", patientId,
                        "scheduleCount", schedules.size(), "hospitalId", item.getHospital().getId()));

        return schedules.stream().map(this::mapScheduleResponse).toList();
    }

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void detectMissedDoses() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime threshold = now.minusMinutes(30);

        List<MedicationSchedule> missed = medicationScheduleRepository
                .findByStatusAndScheduledTimeBefore(MedicationScheduleStatus.PENDING, threshold);

        for (MedicationSchedule schedule : missed) {
            schedule.setStatus(MedicationScheduleStatus.MISSED);
            medicationScheduleRepository.save(schedule);

            eventPublisher.publish(EventConstants.MEDICATION_MISSED, 0L,
                    Map.of("scheduleId", schedule.getId(), "patientId", schedule.getPatientId(),
                            "prescriptionItemId", schedule.getPrescriptionItemId(),
                            "scheduledTime", schedule.getScheduledTime().toString()));
        }

        if (!missed.isEmpty()) {
            log.warn("Marked {} missed medication doses", missed.size());
        }
    }

    @Transactional
    public MedicationScheduleResponse markGiven(Long scheduleId, Long nurseId) {
        MedicationSchedule schedule = medicationScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));

        if (schedule.getStatus() != MedicationScheduleStatus.PENDING) {
            throw new IllegalStateException("Schedule already " + schedule.getStatus());
        }

        OffsetDateTime now = OffsetDateTime.now();
        long diffMinutes = java.time.Duration.between(schedule.getScheduledTime(), now).toMinutes();
        if (Math.abs(diffMinutes) > 120) {
            throw new IllegalStateException("Outside administration tolerance window (120 min)");
        }

        schedule.setStatus(MedicationScheduleStatus.GIVEN);
        schedule.setNurseId(nurseId);
        schedule = medicationScheduleRepository.save(schedule);
        return mapScheduleResponse(schedule);
    }

    @Transactional(readOnly = true)
    public List<MedicationScheduleResponse> getPatientSchedules(Long patientId) {
        return medicationScheduleRepository.findByPatientIdOrderByScheduledTimeAsc(patientId).stream()
                .map(this::mapScheduleResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MedicationScheduleResponse> getPendingSchedules(Long patientId) {
        return medicationScheduleRepository.findByPatientIdOrderByScheduledTimeAsc(patientId).stream()
                .filter(s -> s.getStatus() == MedicationScheduleStatus.PENDING)
                .map(this::mapScheduleResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MedicationSchedule> getMissedSchedules() {
        OffsetDateTime threshold = OffsetDateTime.now().minusMinutes(30);
        return medicationScheduleRepository
                .findByStatusAndScheduledTimeBefore(MedicationScheduleStatus.PENDING, threshold);
    }

    private int parseFrequency(String freq) {
        if (freq.contains("3")) return 3;
        if (freq.contains("2") || freq.contains("bid") || freq.contains("b.i.d")) return 2;
        if (freq.contains("4") || freq.contains("qid") || freq.contains("q.i.d")) return 4;
        if (freq.contains("6") || freq.contains("every.*6") || freq.contains("q6h")) return 4;
        if (freq.contains("8") || freq.contains("every.*8") || freq.contains("q8h")) return 3;
        if (freq.contains("12") || freq.contains("every.*12") || freq.contains("q12h")) return 2;
        if (freq.contains("od") || freq.contains("daily") || freq.contains("once")) return 1;
        return 1;
    }

    private int parseDuration(String duration) {
        try {
            return Integer.parseInt(duration.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 7;
        }
    }

    private List<OffsetDateTime> generateDailyTimes(int dosesPerDay, OffsetDateTime now) {
        OffsetDateTime base = now.withHour(8).withMinute(0).withSecond(0).withNano(0);
        if (base.isBefore(now)) base = base.plusDays(1);

        List<OffsetDateTime> times = new ArrayList<>();
        int intervalHours = 24 / dosesPerDay;
        for (int i = 0; i < dosesPerDay; i++) {
            times.add(base.plusHours((long) i * intervalHours));
        }
        return times;
    }

    private MedicationScheduleResponse mapScheduleResponse(MedicationSchedule s) {
        return new MedicationScheduleResponse(
                s.getId(), s.getPrescriptionItemId(), s.getPatientId(),
                s.getNurseId(), s.getScheduledTime(), s.getStatus(), s.getCreatedAt());
    }
}
