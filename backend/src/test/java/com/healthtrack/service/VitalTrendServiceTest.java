package com.healthtrack.service;

import com.healthtrack.entity.*;
import com.healthtrack.repository.*;
import com.healthtrack.support.PostgresTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class VitalTrendServiceTest extends PostgresTestBase {

    @Autowired private VitalTrendService vitalTrendService;
    @Autowired private VitalRecordRepository vitalRecordRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private UserRepository userRepository;

    private Patient patient;
    private User nurse;

    @BeforeEach
    void setUp() {
        Hospital hospital = hospitalRepository.save(Hospital.builder()
                .name("VitalTrend Hospital").licenseNumber("VT-" + System.nanoTime())
                .contactEmail("vt@test.com").subscriptionTier(SubscriptionTier.MONTHLY)
                .subscriptionStatus(SubscriptionStatus.ACTIVE).build());

        patient = patientRepository.save(Patient.builder()
                .hospital(hospital).firstName("Vital").lastName("Test")
                .phoneNumber("VT-TEST").build());

        nurse = userRepository.save(User.builder()
                .fullName("Nurse Vital").email("nurse-vt-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.NURSE).hospital(hospital).build());
    }

    @Test
    void getTrend_returnsStableForNormalVitals() {
        for (int i = 0; i < 3; i++) {
            vitalRecordRepository.save(VitalRecord.builder()
                    .hospital(patient.getHospital()).patient(patient).nurse(nurse)
                    .bloodPressure("120/80").heartRate(72).temperature(new BigDecimal("36.6"))
                    .spo2(98).alertFlag(false).build());
        }
        var trend = vitalTrendService.getTrend(patient.getId());
        assertThat(trend).isNotNull();
        assertThat(trend.consecutiveAbnormal()).isFalse();
    }

    @Test
    void updateCache_threeConsecutiveAbnormalTriggersEscalation() {
        for (int i = 0; i < 3; i++) {
            VitalRecord abnormal = vitalRecordRepository.save(VitalRecord.builder()
                    .hospital(patient.getHospital()).patient(patient).nurse(nurse)
                    .bloodPressure("160/100").heartRate(130).temperature(new BigDecimal("39.0"))
                    .spo2(88).alertFlag(true).alertReason("Test abnormal")
                    .build());
            vitalTrendService.updateCache(patient.getId(), abnormal);
        }
        assertThat(vitalTrendService.checkConsecutiveAbnormal(patient.getId())).isTrue();
    }
}
