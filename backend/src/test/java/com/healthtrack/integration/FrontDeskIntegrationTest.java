package com.healthtrack.integration;

import com.healthtrack.dto.FrontDeskDtos.*;
import com.healthtrack.dto.BillingDtos.BillResponse;
import com.healthtrack.dto.IpdDtos.AdmissionResponse;
import com.healthtrack.entity.*;
import com.healthtrack.repository.*;
import com.healthtrack.security.UserPrincipal;
import com.healthtrack.service.FrontDeskService;
import com.healthtrack.support.PostgresTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FrontDeskIntegrationTest extends PostgresTestBase {

    @Autowired private FrontDeskService frontDeskService;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private WalkInQueueRepository walkInQueueRepository;
    @Autowired private BedRepository bedRepository;
    @Autowired private AdmissionRepository admissionRepository;

    private Hospital hospital;
    private Patient patient;
    private User doctorUser;
    private Doctor doctor;
    private Bed bed;
    private UserPrincipal receptionistPrincipal;
    private Long doctorId;

    @BeforeEach
    void setUp() {
        hospital = hospitalRepository.save(Hospital.builder()
                .name("Front Desk Integration").licenseNumber("FDI-" + System.nanoTime())
                .contactEmail("fdi@test.com")
                .subscriptionTier(SubscriptionTier.MONTHLY)
                .subscriptionStatus(SubscriptionStatus.ACTIVE).build());

        User receptionist = userRepository.save(User.builder()
                .fullName("Receptionist INT").email("rec-int-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.RECEPTIONIST).hospital(hospital).build());

        doctorUser = userRepository.save(User.builder()
                .fullName("Dr. Integration").email("doc-int-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.DOCTOR).hospital(hospital).build());

        receptionistPrincipal = new UserPrincipal(receptionist);

        patient = patientRepository.save(Patient.builder()
                .hospital(hospital).firstName("E2E").lastName("Patient")
                .phoneNumber("9999999999").build());

        doctor = doctorRepository.save(Doctor.builder()
                .hospital(hospital).user(doctorUser)
                .specialization("General Medicine").consultationFee(BigDecimal.valueOf(300))
                .isAvailable(true).build());
        doctorId = doctor.getId();

        bed = bedRepository.save(Bed.builder()
                .hospital(hospital).wardName("General Ward").bedNumber("G-01")
                .chargePerDay(BigDecimal.valueOf(1000)).isOccupied(false).build());
    }

    @Test
    void walkInsGenerateUniqueTokens() {
        Patient p1 = patientRepository.save(Patient.builder()
                .hospital(hospital).firstName("Token").lastName("User1")
                .phoneNumber("555-TOK-1").build());
        Patient p2 = patientRepository.save(Patient.builder()
                .hospital(hospital).firstName("Token").lastName("User2")
                .phoneNumber("555-TOK-2").build());
        Patient p3 = patientRepository.save(Patient.builder()
                .hospital(hospital).firstName("Token").lastName("User3")
                .phoneNumber("555-TOK-3").build());

        WalkInResponse w1 = frontDeskService.addWalkIn(
                new WalkInRequest(p1.getId(), doctorId, QueuePriority.NORMAL, null),
                receptionistPrincipal);
        WalkInResponse w2 = frontDeskService.addWalkIn(
                new WalkInRequest(p2.getId(), doctorId, QueuePriority.NORMAL, null),
                receptionistPrincipal);
        WalkInResponse w3 = frontDeskService.addWalkIn(
                new WalkInRequest(p3.getId(), doctorId, QueuePriority.NORMAL, null),
                receptionistPrincipal);

        assertThat(w1.tokenNo()).isEqualTo(1);
        assertThat(w2.tokenNo()).isEqualTo(2);
        assertThat(w3.tokenNo()).isEqualTo(3);
        assertThat(w1.status()).isEqualTo(WalkInQueueStatus.WAITING);
    }

    @Test
    void e2ePatientToAdmissionToBillingFlow() {
        // Step 1: Register patient via existing service already done in setUp
        // Step 2: Add walk-in
        WalkInResponse walkIn = frontDeskService.addWalkIn(
                new WalkInRequest(patient.getId(), doctorId, QueuePriority.NORMAL, "Walk-in test"),
                receptionistPrincipal);
        assertThat(walkIn.tokenNo()).isEqualTo(1);
        assertThat(walkIn.status()).isEqualTo(WalkInQueueStatus.WAITING);

        // Step 3: Move to IN_PROGRESS
        WalkInResponse inProgress = frontDeskService.updateQueueStatus(walkIn.id(),
                new QueueStatusUpdateRequest(WalkInQueueStatus.IN_PROGRESS, "Consultation started"),
                receptionistPrincipal);
        assertThat(inProgress.status()).isEqualTo(WalkInQueueStatus.IN_PROGRESS);

        // Step 4: Complete
        WalkInResponse done = frontDeskService.updateQueueStatus(walkIn.id(),
                new QueueStatusUpdateRequest(WalkInQueueStatus.DONE, "Completed"),
                receptionistPrincipal);
        assertThat(done.status()).isEqualTo(WalkInQueueStatus.DONE);

        // Step 5: Initiate admission
        AdmissionResponse admission = frontDeskService.initiateProvisionalAdmission(
                new ProvisionalAdmissionRequest(patient.getId(), bed.getId(), doctorUser.getId(),
                        "Chest pain observation", LocalDate.now()),
                receptionistPrincipal);
        assertThat(admission).isNotNull();
        assertThat(admission.status()).isEqualTo("ADMITTED");
        assertThat(admission.patientName()).contains("E2E");

        // Step 6: Initiate billing
        BillResponse bill = frontDeskService.initiateBilling(
                new BillingInitiateRequest(patient.getId(),
                        List.of(
                            new BillingInitiateRequest.LineItem("Consultation Fee", BigDecimal.valueOf(300), 1),
                            new BillingInitiateRequest.LineItem("Bed Rent", BigDecimal.valueOf(1000), 2),
                            new BillingInitiateRequest.LineItem("ECG", BigDecimal.valueOf(200), 1)
                        )),
                receptionistPrincipal);
        assertThat(bill).isNotNull();
        assertThat(bill.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(2500));
        assertThat(bill.netPayable()).isEqualByComparingTo(BigDecimal.valueOf(2500));
        assertThat(bill.paymentStatus()).isEqualTo("PENDING");
    }

    @Test
    void queueRespectsEmergencyPriority() {
        // Add normal walk-ins first
        Patient p2 = patientRepository.save(Patient.builder()
                .hospital(hospital).firstName("Normal").lastName("First")
                .phoneNumber("555-NORM-1").build());
        Patient p3 = patientRepository.save(Patient.builder()
                .hospital(hospital).firstName("Normal").lastName("Second")
                .phoneNumber("555-NORM-2").build());

        frontDeskService.addWalkIn(
                new WalkInRequest(p2.getId(), doctorId, QueuePriority.NORMAL, null),
                receptionistPrincipal);
        frontDeskService.addWalkIn(
                new WalkInRequest(p3.getId(), doctorId, QueuePriority.NORMAL, null),
                receptionistPrincipal);

        // Now add emergency
        WalkInResponse emergency = frontDeskService.addWalkIn(
                new WalkInRequest(patient.getId(), doctorId, QueuePriority.EMERGENCY, "Chest pain"),
                receptionistPrincipal);

        List<QueueEntry> queue = frontDeskService.getQueue(doctorId, receptionistPrincipal);
        assertThat(queue.get(0).priority()).isEqualTo(QueuePriority.EMERGENCY);
        assertThat(queue.get(0).patientName()).contains("E2E");
        assertThat(queue).hasSize(3);
    }

    @Test
    void dailySummaryCorrectAfterOperations() {
        frontDeskService.addWalkIn(
                new WalkInRequest(patient.getId(), doctorId, QueuePriority.NORMAL, null),
                receptionistPrincipal);

        DailySummaryResponse summary = frontDeskService.getDailySummary(receptionistPrincipal);
        assertThat(summary.totalWalkIns()).isGreaterThanOrEqualTo(1);
        assertThat(summary.doctorQueues()).isNotEmpty();
        assertThat(summary.doctorQueues().get(0).doctorName()).contains("Dr. Integration");
    }

    @Test
    void admissionOccupiesBed() {
        frontDeskService.initiateProvisionalAdmission(
                new ProvisionalAdmissionRequest(patient.getId(), bed.getId(), doctorUser.getId(),
                        "Test admission bed occupancy", LocalDate.now()),
                receptionistPrincipal);

        List<Bed> allBeds = bedRepository.findAll();
        assertThat(allBeds).anyMatch(b -> b.getIsOccupied().equals(true));
    }
}
