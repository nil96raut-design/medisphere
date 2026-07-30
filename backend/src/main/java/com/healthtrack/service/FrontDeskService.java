package com.healthtrack.service;

import com.healthtrack.dto.FrontDeskDtos.*;
import com.healthtrack.dto.IpdDtos.AdmissionResponse;
import com.healthtrack.dto.BillingDtos.BillResponse;
import com.healthtrack.entity.*;
import com.healthtrack.event.EventConstants;
import com.healthtrack.event.EventPublisher;
import com.healthtrack.repository.*;
import com.healthtrack.security.TenantValidator;
import com.healthtrack.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FrontDeskService {

    private final WalkInQueueRepository walkInQueueRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final BedRepository bedRepository;
    private final AdmissionRepository admissionRepository;
    private final BillRepository billRepository;
    private final HospitalRepository hospitalRepository;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final TenantValidator tenantValidator;
    private final EventPublisher eventPublisher;

    @Transactional
    @CacheEvict(value = "queueCache", key = "#request.doctorId()")
    public WalkInResponse addWalkIn(WalkInRequest request, UserPrincipal currentUser) {
        Long hospitalId = currentUser.getHospitalId();
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Hospital not found"));

        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
        tenantValidator.validateHospitalAccess(patient.getHospital().getId(), hospitalId);

        Doctor doctor = doctorRepository.findByIdLocked(request.doctorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found"));
        tenantValidator.validateHospitalAccess(doctor.getHospital().getId(), hospitalId);

        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        LocalDateTime dayEnd = LocalDate.now().atTime(LocalTime.MAX);

        List<WalkInQueue> lastTokens = walkInQueueRepository.findLastTokenLocked(
                request.doctorId(), dayStart, dayEnd);
        int nextToken = lastTokens.stream()
                .mapToInt(WalkInQueue::getTokenNo)
                .max()
                .orElse(0) + 1;

        WalkInQueue entry = WalkInQueue.builder()
                .hospital(hospital)
                .patient(patient)
                .doctor(doctor)
                .createdBy(currentUser.getUser())
                .tokenNo(nextToken)
                .status(WalkInQueueStatus.WAITING)
                .priority(request.priority() != null ? request.priority() : QueuePriority.NORMAL)
                .notes(request.notes())
                .createdAt(LocalDateTime.now())
                .build();

        entry = walkInQueueRepository.save(entry);

        Map<String, Object> payload = Map.of(
                "walkInId", entry.getId(),
                "patientId", patient.getId(),
                "doctorId", doctor.getId(),
                "tokenNo", nextToken,
                "hospitalId", hospitalId);
        eventPublisher.publish(EventConstants.WALK_IN_ADDED, hospitalId, payload);

        return mapToWalkInResponse(entry);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "queueCache", key = "#doctorId", unless = "#result.isEmpty()")
    public List<QueueEntry> getQueue(Long doctorId, UserPrincipal currentUser) {
        tenantValidator.validateHospitalAccess(
                doctorRepository.findById(doctorId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found"))
                        .getHospital().getId(),
                currentUser.getHospitalId());

        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        LocalDateTime dayEnd = LocalDate.now().atTime(LocalTime.MAX);

        List<WalkInQueue> walkIns = walkInQueueRepository
                .findQueueByDoctorAndDateOrdered(doctorId, dayStart, dayEnd);

        return walkIns.stream()
                .filter(w -> w.getStatus() != WalkInQueueStatus.DONE && w.getStatus() != WalkInQueueStatus.NO_SHOW)
                .map(w -> new QueueEntry(
                        w.getId(), "WALK_IN", w.getTokenNo(),
                        w.getPatient().getId(),
                        w.getPatient().getFirstName() + " " + w.getPatient().getLastName(),
                        w.getStatus(), w.getPriority(), w.getCreatedAt(), w.getNotes()))
                .toList();
    }

    @Transactional
    @CacheEvict(value = "queueCache", allEntries = true)
    public WalkInResponse updateQueueStatus(Long queueId, QueueStatusUpdateRequest request, UserPrincipal currentUser) {
        WalkInQueue entry = walkInQueueRepository.findById(queueId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Walk-in entry not found"));
        tenantValidator.validateHospitalAccess(entry.getHospital().getId(), currentUser.getHospitalId());

        if (entry.getStatus() == WalkInQueueStatus.DONE || entry.getStatus() == WalkInQueueStatus.NO_SHOW) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot update status of a " + entry.getStatus().name().toLowerCase().replace('_', ' ') + " entry");
        }

        if (request.status() == WalkInQueueStatus.IN_PROGRESS && entry.getStatus() != WalkInQueueStatus.WAITING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only WAITING entries can be moved to IN_PROGRESS");
        }

        entry.setStatus(request.status());
        if (request.notes() != null) entry.setNotes(request.notes());
        entry = walkInQueueRepository.save(entry);
        return mapToWalkInResponse(entry);
    }

    @Transactional(readOnly = true)
    public DuplicateCheckResponse checkDuplicatePatient(DuplicateCheckRequest request, UserPrincipal currentUser) {
        Long hospitalId = currentUser.getHospitalId();
        List<DuplicateCheckResponse.MatchResult> matches = new ArrayList<>();

        patientRepository.findByPhoneNumber(request.phoneNumber()).ifPresent(p -> {
            if (p.getHospital().getId().equals(hospitalId)) {
                matches.add(new DuplicateCheckResponse.MatchResult(
                        p.getId(),
                        p.getFirstName() + " " + p.getLastName(),
                        p.getPhoneNumber(),
                        p.getEmail(),
                        1.0));
            }
        });

        if (request.firstName() != null && !request.firstName().isBlank()) {
            Page<Patient> similar = patientRepository.searchPatients(
                    request.firstName(), hospitalId, PageRequest.of(0, 5));
            for (Patient p : similar) {
                boolean alreadyMatched = matches.stream().anyMatch(m -> m.patientId().equals(p.getId()));
                if (!alreadyMatched) {
                    double score = computeSimilarity(request.firstName(), request.lastName(), p);
                    if (score > 0.3) {
                        matches.add(new DuplicateCheckResponse.MatchResult(
                                p.getId(), p.getFirstName() + " " + p.getLastName(),
                                p.getPhoneNumber(), p.getEmail(), score));
                    }
                }
            }
        }

        matches.sort((a, b) -> Double.compare(b.score(), a.score()));
        return new DuplicateCheckResponse(!matches.isEmpty(), matches);
    }

    @Transactional
    public AdmissionResponse initiateProvisionalAdmission(ProvisionalAdmissionRequest request, UserPrincipal currentUser) {
        Long hospitalId = currentUser.getHospitalId();
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Hospital not found"));

        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
        tenantValidator.validateHospitalAccess(patient.getHospital().getId(), hospitalId);

        Bed bed = bedRepository.findByIdLocked(request.bedId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bed not found"));
        if (bed.getIsOccupied()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bed is already occupied");
        }

        User doctor = userRepository.findById(request.doctorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found"));
        if (doctor.getRole() != Role.DOCTOR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected user is not a doctor");
        }

        bed.setIsOccupied(true);
        bedRepository.save(bed);

        Admission admission = Admission.builder()
                .hospital(hospital)
                .patient(patient)
                .doctor(doctor)
                .bed(bed)
                .admissionDate(request.admissionDate() != null ? request.admissionDate() : LocalDate.now())
                .initialDiagnosis(request.initialDiagnosis())
                .status(AdmissionStatus.ADMITTED)
                .build();
        admission = admissionRepository.save(admission);

        Map<String, Object> payload = Map.of(
                "admissionId", admission.getId(),
                "patientId", patient.getId(),
                "bedId", bed.getId(),
                "hospitalId", hospitalId);
        eventPublisher.publish(EventConstants.PATIENT_ADMITTED, hospitalId, payload);

        return mapToAdmissionResponse(admission);
    }

    @Transactional
    public BillResponse initiateBilling(BillingInitiateRequest request, UserPrincipal currentUser) {
        Long hospitalId = currentUser.getHospitalId();
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Hospital not found"));

        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
        tenantValidator.validateHospitalAccess(patient.getHospital().getId(), hospitalId);

        String idempotencyKey = "PROV_" + hospitalId + "_" + request.patientId() + "_" + System.nanoTime();

        if (request.items() == null || request.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one line item is required");
        }

        BigDecimal total = request.items().stream()
                .map(item -> item.amount().multiply(BigDecimal.valueOf(item.quantity() != null ? item.quantity() : 1)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Bill bill = Bill.builder()
                .hospital(hospital)
                .patient(patient)
                .idempotencyKey(idempotencyKey)
                .totalAmount(total)
                .discountAmount(BigDecimal.ZERO)
                .insuranceCoveredAmount(BigDecimal.ZERO)
                .netPayable(total)
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        bill = billRepository.save(bill);

        Bill saved = bill;
        Map<String, Object> payload = Map.of(
                "billId", saved.getId(),
                "patientId", patient.getId(),
                "amount", total,
                "hospitalId", hospitalId);
        eventPublisher.publish(EventConstants.BILL_GENERATED, hospitalId, payload);

        return mapToBillResponse(saved, patient);
    }

    @Transactional(readOnly = true)
    public DailySummaryResponse getDailySummary(UserPrincipal currentUser) {
        Long hospitalId = currentUser.getHospitalId();
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        LocalDateTime dayEnd = LocalDate.now().atTime(LocalTime.MAX);

        long totalWalkIns = walkInQueueRepository.countByHospitalIdAndCreatedAtBetween(hospitalId, dayStart, dayEnd);
        long totalAppointments = appointmentRepository.countByHospitalIdAndAppointmentDate(hospitalId, LocalDate.now());
        long newPatients = patientRepository.countByHospitalIdAndCreatedAtBetween(hospitalId, dayStart, dayEnd);
        long emergencyCases = walkInQueueRepository.countByHospitalIdAndCreatedAtBetweenAndPriority(
                hospitalId, dayStart, dayEnd, QueuePriority.EMERGENCY);
        long pendingQueue = walkInQueueRepository.countByHospitalIdAndCreatedAtBetweenAndStatus(
                hospitalId, dayStart, dayEnd, WalkInQueueStatus.WAITING);
        long totalAdmissions = admissionRepository.countByStatus(AdmissionStatus.ADMITTED);
        long totalBillsInitiated = billRepository.countByHospitalIdAndCreatedAtBetween(hospitalId,
                dayStart.atZone(ZoneId.systemDefault()).toOffsetDateTime(),
                dayEnd.atZone(ZoneId.systemDefault()).toOffsetDateTime());

        List<Doctor> doctors = doctorRepository.findByHospitalId(hospitalId);
        List<DailySummaryResponse.DoctorQueueSummary> doctorQueues = doctors.stream()
                .map(d -> {
                    int len = (int) walkInQueueRepository
                            .countByDoctorIdAndCreatedAtBetweenAndStatusNot(
                                    d.getId(), dayStart, dayEnd, WalkInQueueStatus.DONE);
                    int emerg = (int) walkInQueueRepository
                            .countByHospitalIdAndCreatedAtBetweenAndPriority(
                                    hospitalId, dayStart, dayEnd, QueuePriority.EMERGENCY);
                    return new DailySummaryResponse.DoctorQueueSummary(
                            d.getId(), d.getUser().getFullName(), len, emerg);
                })
                .toList();

        return new DailySummaryResponse(
                totalWalkIns, totalAppointments, newPatients, emergencyCases,
                pendingQueue, totalAdmissions, totalBillsInitiated, doctorQueues);
    }

    private double computeSimilarity(String firstName, String lastName, Patient existing) {
        double score = 0.0;
        String inputFirst = firstName.toLowerCase().trim();
        String inputLast = lastName != null ? lastName.toLowerCase().trim() : "";
        String existFirst = existing.getFirstName().toLowerCase().trim();
        String existLast = existing.getLastName().toLowerCase().trim();

        if (inputFirst.equals(existFirst) && inputLast.equals(existLast)) return 1.0;
        if (inputFirst.equals(existFirst)) score += 0.5;
        if (inputLast.equals(existLast)) score += 0.4;
        if (inputFirst.startsWith(existFirst) || existFirst.startsWith(inputFirst)) score += 0.25;
        if (inputLast.startsWith(existLast) || existLast.startsWith(inputLast)) score += 0.15;

        return Math.min(score, 1.0);
    }

    private WalkInResponse mapToWalkInResponse(WalkInQueue w) {
        return new WalkInResponse(
                w.getId(), w.getPatient().getId(),
                w.getPatient().getFirstName() + " " + w.getPatient().getLastName(),
                w.getDoctor().getId(), w.getDoctor().getUser().getFullName(),
                w.getTokenNo(), w.getStatus(), w.getPriority(),
                w.getNotes(), w.getCreatedAt());
    }

    private AdmissionResponse mapToAdmissionResponse(Admission a) {
        return new AdmissionResponse(
                a.getId(), a.getPatient().getId(),
                a.getPatient().getFirstName() + " " + a.getPatient().getLastName(),
                a.getDoctor().getId(), a.getDoctor().getFullName(),
                a.getBed().getId(), a.getBed().getWardName(), a.getBed().getBedNumber(),
                a.getAdmissionDate(), a.getDischargeDate(),
                a.getInitialDiagnosis(), a.getDischargeSummary(),
                a.getStatus().name());
    }

    private BillResponse mapToBillResponse(Bill bill, Patient patient) {
        return new BillResponse(
                bill.getId(), patient.getId(),
                patient.getFirstName() + " " + patient.getLastName(),
                bill.getTotalAmount(), bill.getDiscountAmount(),
                bill.getInsuranceCoveredAmount(), bill.getNetPayable(),
                bill.getPaymentStatus().name(), bill.getPaymentMode() != null ? bill.getPaymentMode().name() : null,
                bill.getCreatedAt(),
                bill.getRefundReason(), bill.getRefundedAt(), bill.getRefundedAmount());
    }
}
