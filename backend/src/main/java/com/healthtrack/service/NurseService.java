package com.healthtrack.service;

import com.healthtrack.dto.ClinicalSafetyDtos.MedicationScheduleResponse;
import com.healthtrack.dto.LabDtos;
import com.healthtrack.dto.NurseDtos.*;
import com.healthtrack.entity.*;
import com.healthtrack.event.EventConstants;
import com.healthtrack.event.EventPublisher;
import com.healthtrack.repository.*;
import com.healthtrack.security.TenantValidator;
import com.healthtrack.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Backoff;
import org.springframework.dao.DataAccessException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "nursePatients")
public class NurseService {

    private final NurseAssignmentRepository nurseAssignmentRepository;
    private final VitalRecordRepository vitalRecordRepository;
    private final MedicationAdministrationRepository medicationAdminRepository;
    private final NursingNoteRepository nursingNoteRepository;
    private final NurseTaskRepository nurseTaskRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final BedRepository bedRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final LabTestOrderRepository labTestOrderRepository;
    private final TenantValidator tenantValidator;
    private final EventPublisher eventPublisher;
    private final IdempotencyService idempotencyService;
    private final VitalTrendService vitalTrendService;
    private final MedicationSchedulingService medicationSchedulingService;
    private final TaskEngineService taskEngineService;
    private final WriteBufferService writeBufferService;

    // ──────────────────────────────────────────────
    // ASSIGNMENTS
    // ──────────────────────────────────────────────

    @Transactional
    @CacheEvict(allEntries = true)
    public NurseAssignmentResponse assignNurse(AssignNurseRequest request, UserPrincipal currentUser) {
        Long hospitalId = currentUser.getHospitalId();

        User nurse = userRepository.findById(request.nurseId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nurse not found"));
        if (nurse.getRole() != Role.NURSE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a nurse");
        }
        tenantValidator.validateHospitalAccess(nurse.getHospital().getId(), hospitalId);

        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
        tenantValidator.validateHospitalAccess(patient.getHospital().getId(), hospitalId);

        if (nurseAssignmentRepository.existsByPatientIdAndStatus(patient.getId(), NurseAssignmentStatus.ACTIVE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Patient already has an active nurse assignment");
        }

        Bed bed = null;
        String wardName = null;
        if (request.bedId() != null) {
            bed = bedRepository.findById(request.bedId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bed not found"));
            tenantValidator.validateHospitalAccess(bed.getHospital().getId(), hospitalId);
            wardName = bed.getWardName();
        }

        NurseAssignment assignment = NurseAssignment.builder()
                .hospital(nurse.getHospital())
                .nurse(nurse)
                .patient(patient)
                .bed(bed)
                .wardName(wardName)
                .status(NurseAssignmentStatus.ACTIVE)
                .build();
        assignment = nurseAssignmentRepository.save(assignment);

        NurseTask vitalTask = NurseTask.builder()
                .hospital(nurse.getHospital())
                .nurse(nurse)
                .patient(patient)
                .taskType(NurseTaskType.VITALS)
                .dueTime(OffsetDateTime.now().plusHours(1))
                .isRecurring(true)
                .recurrenceIntervalMinutes(240)
                .priority(TaskPriority.NORMAL)
                .source("auto")
                .build();
        nurseTaskRepository.save(vitalTask);

        NurseAssignmentResponse response = mapAssignmentResponse(assignment);
        eventPublisher.publish(EventConstants.NURSE_ASSIGNED, hospitalId,
                Map.of("assignmentId", assignment.getId(), "nurseId", request.nurseId(),
                        "patientId", request.patientId(), "hospitalId", hospitalId));

        return response;
    }

    @Transactional(readOnly = true)
    @Cacheable(key = "'nurse:' + #currentUser.getUser().getId()")
    public List<AssignedPatientResponse> getAssignedPatients(UserPrincipal currentUser) {
        Long nurseId = currentUser.getUser().getId();
        List<NurseAssignment> assignments = nurseAssignmentRepository
                .findByNurseIdAndStatusFetching(nurseId, NurseAssignmentStatus.ACTIVE);

        return assignments.stream().map(a -> {
            List<NurseTaskResponse> pendingTasks = nurseTaskRepository
                    .findByPatientIdOrderByCreatedAtDesc(a.getPatient().getId()).stream()
                    .filter(t -> t.getStatus() != NurseTaskStatus.DONE)
                    .map(this::mapTaskResponse)
                    .toList();

            return new AssignedPatientResponse(
                    a.getPatient().getId(),
                    a.getPatient().getFirstName() + " " + a.getPatient().getLastName(),
                    a.getBed() != null ? a.getBed().getId() : null,
                    a.getBed() != null ? a.getBed().getBedNumber() : null,
                    a.getWardName(),
                    a.getAssignedAt(),
                    pendingTasks);
        }).toList();
    }

    @Transactional
    @CacheEvict(allEntries = true)
    public void releaseAssignment(Long assignmentId, UserPrincipal currentUser) {
        NurseAssignment assignment = nurseAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));
        tenantValidator.validateHospitalAccess(assignment.getHospital().getId(), currentUser.getHospitalId());

        if (assignment.getStatus() != NurseAssignmentStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignment is already released");
        }
        assignment.setStatus(NurseAssignmentStatus.RELEASED);
        assignment.setReleasedAt(OffsetDateTime.now());
        nurseAssignmentRepository.save(assignment);
    }

    // ──────────────────────────────────────────────
    // VITALS
    // ──────────────────────────────────────────────

    @Transactional(timeout = 5)
    @Retryable(
            retryFor = {DataAccessException.class, org.hibernate.exception.JDBCConnectionException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2.0)
    )
    public VitalRecordResponse recordVitals(VitalRecordRequest request, UserPrincipal currentUser) {
        String requestId = request.toString();
        if (!idempotencyService.tryProcess(requestId, currentUser.getHospitalId(), "RECORD_VITALS")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate request");
        }

        Long hospitalId = currentUser.getHospitalId();
        User nurse = currentUser.getUser();
        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
        tenantValidator.validateHospitalAccess(patient.getHospital().getId(), hospitalId);

        if (!nurseAssignmentRepository.existsByPatientIdAndStatus(patient.getId(), NurseAssignmentStatus.ACTIVE)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Patient is not assigned to a nurse");
        }

        String alertReason = detectAbnormalVitals(request);
        boolean alertFlag = alertReason != null;

        VitalRecord record = VitalRecord.builder()
                .hospital(patient.getHospital())
                .patient(patient)
                .nurse(nurse)
                .bloodPressure(request.bloodPressure())
                .heartRate(request.heartRate())
                .temperature(request.temperature())
                .spo2(request.spo2())
                .sugarLevel(request.sugarLevel())
                .alertFlag(alertFlag)
                .alertReason(alertReason)
                .build();
        record = vitalRecordRepository.save(record);

        vitalTrendService.updateCache(patient.getId(), record);

        eventPublisher.publish(EventConstants.VITALS_RECORDED, hospitalId,
                Map.of("vitalId", record.getId(), "patientId", patient.getId(),
                        "nurseId", nurse.getId(), "hospitalId", hospitalId,
                        "alertFlag", alertFlag));

        if (alertFlag) {
            eventPublisher.publish(EventConstants.CRITICAL_VITAL, hospitalId,
                    Map.of("vitalId", record.getId(), "patientId", patient.getId(),
                            "nurseId", nurse.getId(), "alertReason", alertReason,
                            "hospitalId", hospitalId));
        }

        return mapVitalResponse(record);
    }

    @Recover
    public VitalRecordResponse recoverRecordVitals(Exception e, VitalRecordRequest request, UserPrincipal currentUser) {
        String alertReason = detectAbnormalVitals(request);
        boolean alertFlag = alertReason != null;

        Map<String, Object> payload = new HashMap<>();
        payload.put("request", request);
        payload.put("userId", currentUser.getUser().getId());
        payload.put("hospitalId", currentUser.getHospitalId());

        writeBufferService.queueFailedWrite("RECORD_VITALS", payload, QueuePriority.CRITICAL);

        return new VitalRecordResponse(
                -1L, request.patientId(), "Patient Name Unavailable",
                currentUser.getUser().getId(), currentUser.getUser().getFullName(),
                request.bloodPressure(), request.heartRate(), request.temperature(),
                request.spo2(), request.sugarLevel(),
                alertFlag, alertReason, OffsetDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<VitalRecordResponse> getVitals(Long patientId, UserPrincipal currentUser) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
        tenantValidator.validateHospitalAccess(patient.getHospital().getId(), currentUser.getHospitalId());
        return vitalRecordRepository.findByPatientIdOrderByRecordedAtDesc(patientId).stream()
                .map(this::mapVitalResponse)
                .toList();
    }

    // ──────────────────────────────────────────────
    // MEDICATION ADMINISTRATION
    // ──────────────────────────────────────────────

    @Transactional
    public MedicationAdminResponse administerMedication(MedicationAdminRequest request, UserPrincipal currentUser) {
        String requestKey = "med_admin_" + request.prescriptionItemId() + "_" + currentUser.getUser().getId();
        if (!idempotencyService.tryProcess(requestKey, currentUser.getHospitalId(), "ADMINISTER_MEDICATION")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate request");
        }

        Long hospitalId = currentUser.getHospitalId();
        User nurse = currentUser.getUser();
        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
        tenantValidator.validateHospitalAccess(patient.getHospital().getId(), hospitalId);

        PrescriptionItem prescriptionItem = prescriptionItemRepository.findById(request.prescriptionItemId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prescription item not found"));
        tenantValidator.validateHospitalAccess(prescriptionItem.getHospital().getId(), hospitalId);

        if (medicationAdminRepository.existsByPrescriptionItemId(request.prescriptionItemId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Medication already administered for this prescription");
        }

        MedicationAdministration admin = MedicationAdministration.builder()
                .hospital(patient.getHospital())
                .prescriptionItem(prescriptionItem)
                .patient(patient)
                .nurse(nurse)
                .status(MedicationStatus.GIVEN)
                .notes(request.notes())
                .build();
        admin = medicationAdminRepository.save(admin);

        List<MedicationScheduleResponse> schedules = medicationSchedulingService
                .getPendingSchedules(request.patientId());
        for (MedicationScheduleResponse s : schedules) {
            try {
                medicationSchedulingService.markGiven(s.id(), nurse.getId());
            } catch (Exception ignored) {}
        }

        completePendingMedicationTasks(nurse, patient);
        eventPublisher.publish(EventConstants.MEDICATION_GIVEN, hospitalId,
                Map.of("adminId", admin.getId(), "patientId", patient.getId(),
                        "nurseId", nurse.getId(), "medicineName", prescriptionItem.getMedicineName(),
                        "prescriptionItemId", request.prescriptionItemId(),
                        "hospitalId", hospitalId));

        return mapMedicationResponse(admin, prescriptionItem);
    }

    @Transactional(readOnly = true)
    public List<MedicationAdminResponse> getMedicationHistory(Long patientId, UserPrincipal currentUser) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
        tenantValidator.validateHospitalAccess(patient.getHospital().getId(), currentUser.getHospitalId());
        return medicationAdminRepository.findByPatientIdOrderByAdministeredAtDesc(patientId).stream()
                .map(m -> {
                    PrescriptionItem pi = m.getPrescriptionItem();
                    return mapMedicationResponse(m, pi);
                })
                .toList();
    }

    // ──────────────────────────────────────────────
    // NURSING NOTES
    // ──────────────────────────────────────────────

    @Transactional
    public NursingNoteResponse addNursingNote(NursingNoteRequest request, UserPrincipal currentUser) {
        Long hospitalId = currentUser.getHospitalId();
        User nurse = currentUser.getUser();
        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
        tenantValidator.validateHospitalAccess(patient.getHospital().getId(), hospitalId);

        NursingNote note = NursingNote.builder()
                .hospital(patient.getHospital())
                .patient(patient)
                .nurse(nurse)
                .note(request.note())
                .build();
        note = nursingNoteRepository.save(note);

        eventPublisher.publish(EventConstants.NURSE_NOTE_ADDED, hospitalId,
                Map.of("noteId", note.getId(), "patientId", patient.getId(),
                        "nurseId", nurse.getId(), "hospitalId", hospitalId));

        return mapNoteResponse(note);
    }

    @Transactional(readOnly = true)
    public List<NursingNoteResponse> getNursingNotes(Long patientId, UserPrincipal currentUser) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
        tenantValidator.validateHospitalAccess(patient.getHospital().getId(), currentUser.getHospitalId());
        return nursingNoteRepository.findByPatientIdOrderByCreatedAtDesc(patientId).stream()
                .map(this::mapNoteResponse)
                .toList();
    }

    // ──────────────────────────────────────────────
    // TASKS
    // ──────────────────────────────────────────────

    @Transactional
    public NurseTaskResponse createTask(NurseTaskRequest request, UserPrincipal currentUser) {
        Long hospitalId = currentUser.getHospitalId();
        User nurse = userRepository.findById(request.nurseId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nurse not found"));
        tenantValidator.validateHospitalAccess(nurse.getHospital().getId(), hospitalId);

        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
        tenantValidator.validateHospitalAccess(patient.getHospital().getId(), hospitalId);

        NurseTask task = NurseTask.builder()
                .hospital(nurse.getHospital())
                .nurse(nurse)
                .patient(patient)
                .taskType(request.taskType())
                .dueTime(request.dueTime())
                .isRecurring(request.isRecurring() != null && request.isRecurring())
                .recurrenceIntervalMinutes(request.recurrenceIntervalMinutes())
                .priority(request.priority() != null ? request.priority() : TaskPriority.NORMAL)
                .source(request.source() != null ? request.source() : "manual")
                .build();
        task = nurseTaskRepository.save(task);

        eventPublisher.publish(EventConstants.NURSE_TASK_CREATED, hospitalId,
                Map.of("taskId", task.getId(), "nurseId", request.nurseId(),
                        "patientId", request.patientId(), "taskType", request.taskType().name(),
                        "hospitalId", hospitalId));

        return mapTaskResponse(task);
    }

    @Transactional(readOnly = true)
    public List<NurseTaskResponse> getTasks(UserPrincipal currentUser) {
        Long nurseId = currentUser.getUser().getId();
        return nurseTaskRepository.findByNurseIdFetching(nurseId).stream()
                .map(this::mapTaskResponse)
                .toList();
    }

    @Transactional
    public NurseTaskResponse updateTaskStatus(Long taskId, NurseTaskStatus status, UserPrincipal currentUser) {
        NurseTask task = nurseTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        tenantValidator.validateHospitalAccess(task.getHospital().getId(), currentUser.getHospitalId());

        if (task.getStatus() == NurseTaskStatus.DONE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Task is already completed");
        }
        task.setStatus(status);
        task.setCompletedAt(status == NurseTaskStatus.DONE ? OffsetDateTime.now() : null);
        task = nurseTaskRepository.save(task);

        if (status == NurseTaskStatus.DONE) {
            eventPublisher.publish(EventConstants.NURSE_TASK_COMPLETED, currentUser.getHospitalId(),
                    Map.of("taskId", task.getId(), "nurseId", currentUser.getUser().getId(),
                            "patientId", task.getPatient().getId(), "hospitalId", currentUser.getHospitalId()));
        }
        return mapTaskResponse(task);
    }

    // ──────────────────────────────────────────────
    // LAB COLLECTION INTEGRATION
    // ──────────────────────────────────────────────

    @Transactional
    public LabDtos.LabOrderResponse collectLabSample(Long orderId,
                                                                           LabDtos.SampleCollectionRequest request,
                                                                           UserPrincipal currentUser) {
        Long hospitalId = currentUser.getHospitalId();

        LabTestOrder order = labTestOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab order not found"));
        tenantValidator.validateHospitalAccess(order.getHospital().getId(), hospitalId);

        if (order.getStatus() != LabOrderStatus.ORDERED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sample already collected for this order");
        }

        order.setStatus(LabOrderStatus.SAMPLE_COLLECTED);
        order.setTechnicianNotes(request.technicianNotes());
        order.setSampleCollectedAt(LocalDateTime.now());
        order = labTestOrderRepository.save(order);

        eventPublisher.publish(EventConstants.SAMPLE_COLLECTED, hospitalId,
                Map.of("orderId", order.getId(), "patientId", order.getPatient().getId(),
                        "nurseId", currentUser.getUser().getId(), "hospitalId", hospitalId));

        return mapLabOrderResponse(order);
    }

    // ──────────────────────────────────────────────
    // INTERNAL HELPERS
    // ──────────────────────────────────────────────

    private void completePendingMedicationTasks(User nurse, Patient patient) {
        List<NurseTask> medTasks = nurseTaskRepository.findByPatientIdOrderByCreatedAtDesc(patient.getId())
                .stream()
                .filter(t -> t.getTaskType() == NurseTaskType.MEDICATION && t.getStatus() != NurseTaskStatus.DONE)
                .toList();
        for (NurseTask t : medTasks) {
            t.setStatus(NurseTaskStatus.DONE);
            t.setCompletedAt(OffsetDateTime.now());
        }
        nurseTaskRepository.saveAll(medTasks);
    }

    private String detectAbnormalVitals(VitalRecordRequest request) {
        List<String> alerts = new ArrayList<>();

        if (request.bloodPressure() != null && !request.bloodPressure().isBlank()) {
            try {
                String[] parts = request.bloodPressure().split("/");
                if (parts.length == 2) {
                    int systolic = Integer.parseInt(parts[0].trim());
                    int diastolic = Integer.parseInt(parts[1].trim());
                    if (systolic > 140) alerts.add("Systolic BP > 140");
                    if (diastolic > 90) alerts.add("Diastolic BP > 90");
                }
            } catch (NumberFormatException ignored) {}
        }

        if (request.spo2() != null && request.spo2() < 92) {
            alerts.add("SpO2 < 92%");
        }

        if (request.heartRate() != null && request.heartRate() > 120) {
            alerts.add("Heart Rate > 120 bpm");
        }

        if (request.heartRate() != null && request.heartRate() < 60) {
            alerts.add("Heart Rate < 60 bpm");
        }

        if (request.temperature() != null && request.temperature().compareTo(new BigDecimal("38.5")) > 0) {
            alerts.add("Temperature > 38.5°C");
        }

        return alerts.isEmpty() ? null : String.join("; ", alerts);
    }

    private NurseAssignmentResponse mapAssignmentResponse(NurseAssignment a) {
        return new NurseAssignmentResponse(
                a.getId(), a.getNurse().getId(), a.getNurse().getFullName(),
                a.getPatient().getId(), a.getPatient().getFirstName() + " " + a.getPatient().getLastName(),
                a.getBed() != null ? a.getBed().getId() : null,
                a.getBed() != null ? a.getBed().getBedNumber() : null,
                a.getWardName(),
                a.getStatus(), a.getAssignedAt(), a.getReleasedAt());
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

    private MedicationAdminResponse mapMedicationResponse(MedicationAdministration m, PrescriptionItem pi) {
        return new MedicationAdminResponse(
                m.getId(), m.getPrescriptionItem().getId(),
                m.getPatient().getId(), m.getPatient().getFirstName() + " " + m.getPatient().getLastName(),
                m.getNurse().getId(), m.getNurse().getFullName(),
                pi.getMedicineName(), m.getStatus(), m.getAdministeredAt(), m.getNotes());
    }

    private NursingNoteResponse mapNoteResponse(NursingNote n) {
        return new NursingNoteResponse(
                n.getId(), n.getPatient().getId(),
                n.getPatient().getFirstName() + " " + n.getPatient().getLastName(),
                n.getNurse().getId(), n.getNurse().getFullName(),
                n.getNote(), n.getCreatedAt());
    }

    private NurseTaskResponse mapTaskResponse(NurseTask t) {
        return new NurseTaskResponse(
                t.getId(), t.getNurse().getId(), t.getNurse().getFullName(),
                t.getPatient().getId(), t.getPatient().getFirstName() + " " + t.getPatient().getLastName(),
                t.getTaskType(), t.getStatus(), t.getDueTime(), t.getCompletedAt(), t.getCreatedAt(),
                t.getIsRecurring(), t.getRecurrenceIntervalMinutes(), t.getPriority(), t.getSource());
    }

    private LabDtos.LabOrderResponse mapLabOrderResponse(LabTestOrder o) {
        return new LabDtos.LabOrderResponse(
                o.getId(), o.getPatient().getId(),
                o.getPatient().getFirstName() + " " + o.getPatient().getLastName(),
                o.getTestName(),
                o.getRequestedBy().getFullName(),
                o.getStatus().name(), o.getResultValues(), o.getTechnicianNotes(),
                o.getCompletedAt(), o.getCreatedAt(), o.getSampleCollectedAt(),
                o.getProcessingStartedAt(), o.getProcessingCompletedAt(),
                o.getSampleBarcode(), o.getSampleStorageLocation(),
                o.getCriticalFlag(), o.getTurnaroundMinutes(),
                o.getRetestOf() != null ? o.getRetestOf().getId() : null,
                o.getCorrectionReason(),
                o.getResultEnteredBy() != null ? o.getResultEnteredBy().getFullName() : null,
                o.getPrice(), List.of());
    }
}
