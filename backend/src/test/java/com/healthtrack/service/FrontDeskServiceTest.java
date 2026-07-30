package com.healthtrack.service;

import com.healthtrack.dto.FrontDeskDtos.*;
import com.healthtrack.dto.BillingDtos.BillResponse;
import com.healthtrack.entity.*;
import com.healthtrack.repository.*;
import com.healthtrack.security.UserPrincipal;
import com.healthtrack.support.PostgresTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FrontDeskServiceTest extends PostgresTestBase {

    @Autowired private FrontDeskService frontDeskService;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private WalkInQueueRepository walkInQueueRepository;
    @Autowired private BedRepository bedRepository;

    private Hospital hospital;
    private Patient patient1;
    private Patient patient2;
    private Doctor doctor;
    private UserPrincipal receptionistPrincipal;
    private UserPrincipal adminPrincipal;
    private Long doctorId;

    @BeforeEach
    void setUp() {
        hospital = hospitalRepository.save(Hospital.builder()
                .name("Front Desk Test Hospital").licenseNumber("FD-" + System.nanoTime())
                .contactEmail("fd-test@test.com")
                .subscriptionTier(SubscriptionTier.MONTHLY)
                .subscriptionStatus(SubscriptionStatus.ACTIVE).build());

        User receptionist = userRepository.save(User.builder()
                .fullName("Receptionist Test").email("receptionist-fd-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.RECEPTIONIST).hospital(hospital).build());

        User admin = userRepository.save(User.builder()
                .fullName("Admin FD").email("admin-fd-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.ADMIN).hospital(hospital).build());

        User docUser = userRepository.save(User.builder()
                .fullName("Dr. FD Test").email("doc-fd-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.DOCTOR).hospital(hospital).build());

        receptionistPrincipal = new UserPrincipal(receptionist);
        adminPrincipal = new UserPrincipal(admin);

        patient1 = patientRepository.save(Patient.builder()
                .hospital(hospital).firstName("John").lastName("Doe")
                .phoneNumber("1111111111").email("john@test.com")
                .gender("Male").dateOfBirth(LocalDate.of(1985, 3, 15))
                .build());
        patient2 = patientRepository.save(Patient.builder()
                .hospital(hospital).firstName("Jane").lastName("Doe")
                .phoneNumber("2222222222").email("jane@test.com")
                .build());

        doctor = doctorRepository.save(Doctor.builder()
                .hospital(hospital).user(docUser)
                .specialization("Cardiology").consultationFee(BigDecimal.valueOf(500))
                .isAvailable(true).build());
        doctorId = doctor.getId();
    }

    @Test
    void addWalkInGeneratesSequentialTokens() {
        WalkInResponse w1 = frontDeskService.addWalkIn(
                new WalkInRequest(patient1.getId(), doctorId, QueuePriority.NORMAL, "First patient"),
                receptionistPrincipal);
        WalkInResponse w2 = frontDeskService.addWalkIn(
                new WalkInRequest(patient2.getId(), doctorId, QueuePriority.NORMAL, "Second patient"),
                receptionistPrincipal);

        assertThat(w1.tokenNo()).isEqualTo(1);
        assertThat(w2.tokenNo()).isEqualTo(2);
        assertThat(w1.status()).isEqualTo(WalkInQueueStatus.WAITING);
        assertThat(w2.status()).isEqualTo(WalkInQueueStatus.WAITING);
    }

    @Test
    void priorityQueueOrdering() {
        WalkInResponse normal = frontDeskService.addWalkIn(
                new WalkInRequest(patient1.getId(), doctorId, QueuePriority.NORMAL, null),
                receptionistPrincipal);
        WalkInResponse emergency = frontDeskService.addWalkIn(
                new WalkInRequest(patient2.getId(), doctorId, QueuePriority.EMERGENCY, "Emergency case"),
                receptionistPrincipal);

        List<QueueEntry> queue = frontDeskService.getQueue(doctorId, receptionistPrincipal);
        assertThat(queue).hasSize(2);
        assertThat(queue.get(0).priority()).isEqualTo(QueuePriority.EMERGENCY);
        assertThat(queue.get(0).tokenNo()).isEqualTo(emergency.tokenNo());
        assertThat(queue.get(1).priority()).isEqualTo(QueuePriority.NORMAL);
    }

    @Test
    void walkInStatusTransitions() {
        WalkInResponse walkIn = frontDeskService.addWalkIn(
                new WalkInRequest(patient1.getId(), doctorId, QueuePriority.NORMAL, null),
                receptionistPrincipal);

        WalkInResponse inProgress = frontDeskService.updateQueueStatus(walkIn.id(),
                new QueueStatusUpdateRequest(WalkInQueueStatus.IN_PROGRESS, "Started consultation"),
                receptionistPrincipal);
        assertThat(inProgress.status()).isEqualTo(WalkInQueueStatus.IN_PROGRESS);

        WalkInResponse done = frontDeskService.updateQueueStatus(walkIn.id(),
                new QueueStatusUpdateRequest(WalkInQueueStatus.DONE, "Completed"),
                receptionistPrincipal);
        assertThat(done.status()).isEqualTo(WalkInQueueStatus.DONE);
    }

    @Test
    void cannotUpdateTerminalStatus() {
        WalkInResponse walkIn = frontDeskService.addWalkIn(
                new WalkInRequest(patient1.getId(), doctorId, QueuePriority.NORMAL, null),
                receptionistPrincipal);
        frontDeskService.updateQueueStatus(walkIn.id(),
                new QueueStatusUpdateRequest(WalkInQueueStatus.NO_SHOW, null),
                receptionistPrincipal);

        assertThatThrownBy(() ->
            frontDeskService.updateQueueStatus(walkIn.id(),
                    new QueueStatusUpdateRequest(WalkInQueueStatus.DONE, null),
                    receptionistPrincipal))
            .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void duplicateDetectionByPhone() {
        DuplicateCheckResponse result = frontDeskService.checkDuplicatePatient(
                new DuplicateCheckRequest("1111111111", null, null), receptionistPrincipal);
        assertThat(result.duplicateExists()).isTrue();
        assertThat(result.matches()).hasSize(1);
        assertThat(result.matches().get(0).score()).isEqualTo(1.0);
    }

    @Test
    void duplicateDetectionByName() {
        DuplicateCheckResponse result = frontDeskService.checkDuplicatePatient(
                new DuplicateCheckRequest("9999999999", "John", "Doe"), receptionistPrincipal);
        assertThat(result.duplicateExists()).isTrue();
        assertThat(result.matches()).anyMatch(m -> m.fullName().contains("John"));
    }

    @Test
    void noDuplicateForNewPatient() {
        DuplicateCheckResponse result = frontDeskService.checkDuplicatePatient(
                new DuplicateCheckRequest("9999999999", "Unknown", "Person"), receptionistPrincipal);
        assertThat(result.duplicateExists()).isFalse();
        assertThat(result.matches()).isEmpty();
    }

    @Test
    void dailySummaryReturnsCounts() {
        frontDeskService.addWalkIn(
                new WalkInRequest(patient1.getId(), doctorId, QueuePriority.NORMAL, null),
                receptionistPrincipal);
        frontDeskService.addWalkIn(
                new WalkInRequest(patient2.getId(), doctorId, QueuePriority.EMERGENCY, null),
                receptionistPrincipal);

        DailySummaryResponse summary = frontDeskService.getDailySummary(receptionistPrincipal);
        assertThat(summary.totalWalkIns()).isGreaterThanOrEqualTo(2);
        assertThat(summary.emergencyCases()).isGreaterThanOrEqualTo(1);
        assertThat(summary.doctorQueues()).isNotEmpty();
    }

    @Test
    void initiateBillingCreatesProvisionalBill() {
        BillResponse bill = frontDeskService.initiateBilling(
                new BillingInitiateRequest(patient1.getId(),
                        List.of(new BillingInitiateRequest.LineItem("Consultation", BigDecimal.valueOf(500), 1))),
                receptionistPrincipal);
        assertThat(bill).isNotNull();
        assertThat(bill.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(bill.paymentStatus()).isEqualTo("PENDING");
    }

    @Test
    void queueFiltersDonePatients() {
        WalkInResponse w1 = frontDeskService.addWalkIn(
                new WalkInRequest(patient1.getId(), doctorId, QueuePriority.NORMAL, null),
                receptionistPrincipal);
        WalkInResponse w2 = frontDeskService.addWalkIn(
                new WalkInRequest(patient2.getId(), doctorId, QueuePriority.NORMAL, null),
                receptionistPrincipal);

        frontDeskService.updateQueueStatus(w1.id(),
                new QueueStatusUpdateRequest(WalkInQueueStatus.DONE, null),
                receptionistPrincipal);

        List<QueueEntry> queue = frontDeskService.getQueue(doctorId, receptionistPrincipal);
        assertThat(queue).hasSize(1);
        assertThat(queue.get(0).tokenNo()).isEqualTo(w2.tokenNo());
    }
}
