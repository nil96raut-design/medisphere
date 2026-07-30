package com.healthtrack.service;

import com.healthtrack.dto.LabDtos.*;
import com.healthtrack.entity.*;
import com.healthtrack.repository.*;
import com.healthtrack.security.UserPrincipal;
import com.healthtrack.support.PostgresTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LabServiceTest extends PostgresTestBase {

    @Autowired private LabService labService;
    @Autowired private LabTestOrderRepository labTestOrderRepository;
    @Autowired private LabCriticalRuleRepository labCriticalRuleRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private UserRepository userRepository;

    private Hospital hospital;
    private User labTech;
    private User doctor;
    private Patient patient;
    private UserPrincipal techPrincipal;
    private UserPrincipal doctorPrincipal;

    @BeforeEach
    void setUp() {
        hospital = hospitalRepository.save(Hospital.builder()
                .name("Lab Test Hospital").licenseNumber("LT-" + System.nanoTime())
                .contactEmail("lt@test.com").subscriptionTier(SubscriptionTier.MONTHLY)
                .subscriptionStatus(SubscriptionStatus.ACTIVE).build());

        labTech = userRepository.save(User.builder()
                .fullName("Lab Tech").email("labtech-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.LAB_TECH).hospital(hospital).build());

        doctor = userRepository.save(User.builder()
                .fullName("Dr. Approve").email("drapprove-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.DOCTOR).hospital(hospital).build());

        patient = patientRepository.save(Patient.builder()
                .hospital(hospital).firstName("Lab").lastName("Patient")
                .phoneNumber("LAB-PAT").build());

        techPrincipal = new UserPrincipal(labTech);
        doctorPrincipal = new UserPrincipal(doctor);
    }

    @Test
    void fullWorkflow_ordersToApproved() {
        var sampleReq = new SampleCollectionRequest("Collected in EDTA tube",
                "Blood", "EDTA tube", "BAR-001", "5mL",
                "Venipuncture", "Rack A-1", "2-8C");
        var processReq = new ProcessRequest("Centrifuged at 3000 rpm");
        var resultsReq = new ResultEntryRequest("Hemoglobin: 14.2 g/dL\nWBC: 7.5 x10^3/uL", "Normal range", null);

        LabTestOrder order = labTestOrderRepository.save(LabTestOrder.builder()
                .hospital(hospital).patient(patient).testName("Complete Blood Count")
                .requestedBy(doctor).price(BigDecimal.valueOf(50)).build());

        assertThat(order.getStatus()).isEqualTo(LabOrderStatus.ORDERED);
        assertThat(order.getCriticalFlag()).isFalse();

        var collected = labService.markSampleCollected(order.getId(), sampleReq, techPrincipal);
        assertThat(collected.status()).isEqualTo("SAMPLE_COLLECTED");
        assertThat(collected.sampleBarcode()).isEqualTo("BAR-001");

        var processing = labService.startProcessing(order.getId(), processReq, techPrincipal);
        assertThat(processing.status()).isEqualTo("PROCESSING");

        var entered = labService.enterResults(order.getId(), resultsReq, techPrincipal);
        assertThat(entered.status()).isEqualTo("RESULT_ENTERED");

        var approved = labService.approve(order.getId(), new ApproveRequest("Looks good"), doctorPrincipal);
        assertThat(approved.status()).isEqualTo("APPROVED");
    }

    @Test
    void sampleCollection_withTracking() {
        LabTestOrder order = labTestOrderRepository.save(LabTestOrder.builder()
                .hospital(hospital).patient(patient).testName("Lipid Profile")
                .requestedBy(doctor).build());

        var request = new SampleCollectionRequest("Fasting sample",
                "Blood", "Gel tube", "BAR-002", "3mL",
                "Venipuncture", "Rack B-2", "Room temp");

        var result = labService.markSampleCollected(order.getId(), request, techPrincipal);
        assertThat(result.sampleTrackings()).isNotEmpty();
        assertThat(result.sampleTrackings().get(0).sampleType()).isEqualTo("Blood");
        assertThat(result.sampleTrackings().get(0).barcode()).isEqualTo("BAR-002");
    }

    @Test
    void retestFlow() {
        LabTestOrder order = labTestOrderRepository.save(LabTestOrder.builder()
                .hospital(hospital).patient(patient).testName("Glucose Test")
                .requestedBy(doctor).build());

        labService.markSampleCollected(order.getId(), new SampleCollectionRequest("ok", null, null, null, null, null, null, null), techPrincipal);
        labService.startProcessing(order.getId(), new ProcessRequest("ok"), techPrincipal);
        labService.enterResults(order.getId(), new ResultEntryRequest("Glucose: 95 mg/dL", null, null), techPrincipal);

        var retested = labService.requestRetest(order.getId(), new RetestRequest("Sample hemolyzed, repeat required"), techPrincipal);
        assertThat(retested.status()).isEqualTo("NEEDS_RETEST");
        assertThat(retested.correctionReason()).contains("hemolyzed");
    }

    @Test
    void approve_onlyDoctorsAllowed() {
        LabTestOrder order = labTestOrderRepository.save(LabTestOrder.builder()
                .hospital(hospital).patient(patient).testName("Test")
                .requestedBy(doctor).build());

        labService.markSampleCollected(order.getId(), new SampleCollectionRequest("ok", null, null, null, null, null, null, null), techPrincipal);
        labService.startProcessing(order.getId(), new ProcessRequest("ok"), techPrincipal);
        labService.enterResults(order.getId(), new ResultEntryRequest("Result: 1.0", null, null), techPrincipal);

        assertThatThrownBy(() -> labService.approve(order.getId(), new ApproveRequest(""), techPrincipal))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void wrongStateTransition_throws() {
        LabTestOrder order = labTestOrderRepository.save(LabTestOrder.builder()
                .hospital(hospital).patient(patient).testName("Test")
                .requestedBy(doctor).build());

        assertThatThrownBy(() -> labService.startProcessing(order.getId(), new ProcessRequest(""), techPrincipal))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void criticalResult_detected() {
        labCriticalRuleRepository.save(LabCriticalRule.builder()
                .hospital(hospital).testName("Critical Test").parameterName("Hemoglobin")
                .conditionOperator("<").thresholdValue("10").unit("g/dL")
                .severity("HIGH").build());

        LabTestOrder order = labTestOrderRepository.save(LabTestOrder.builder()
                .hospital(hospital).patient(patient).testName("Critical Test")
                .requestedBy(doctor).build());

        labService.markSampleCollected(order.getId(), new SampleCollectionRequest("ok", null, null, null, null, null, null, null), techPrincipal);
        labService.startProcessing(order.getId(), new ProcessRequest("ok"), techPrincipal);
        var result = labService.enterResults(order.getId(),
                new ResultEntryRequest("Hemoglobin: 8.5 g/dL\nWBC: 7.5", null, null), techPrincipal);

        assertThat(result.criticalFlag()).isTrue();
    }

    @Test
    void cancelOrder() {
        LabTestOrder order = labTestOrderRepository.save(LabTestOrder.builder()
                .hospital(hospital).patient(patient).testName("Cancel Test")
                .requestedBy(doctor).build());

        var cancelled = labService.cancel(order.getId(), techPrincipal);
        assertThat(cancelled.status()).isEqualTo("CANCELLED");
    }

    @Test
    void getTechQueue_returnsSections() {
        LabTestOrder o1 = labTestOrderRepository.save(LabTestOrder.builder()
                .hospital(hospital).patient(patient).testName("Queue Test 1")
                .requestedBy(doctor).build());

        LabTestOrder o2 = labTestOrderRepository.save(LabTestOrder.builder()
                .hospital(hospital).patient(patient).testName("Queue Test 2")
                .requestedBy(doctor).build());
        labService.markSampleCollected(o2.getId(), new SampleCollectionRequest("ok", null, null, null, null, null, null, null), techPrincipal);

        var queue = labService.getTechQueue(techPrincipal);
        assertThat(queue.pendingCollection()).hasSize(1);
        assertThat(queue.pendingCollection().get(0).testName()).isEqualTo("Queue Test 1");
        assertThat(queue.inProcessing()).hasSize(1);
        assertThat(queue.inProcessing().get(0).testName()).isEqualTo("Queue Test 2");
    }
}
