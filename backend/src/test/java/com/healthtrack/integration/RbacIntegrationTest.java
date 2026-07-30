package com.healthtrack.integration;

import com.healthtrack.entity.*;
import com.healthtrack.repository.*;
import com.healthtrack.security.JwtService;
import com.healthtrack.security.UserPrincipal;
import com.healthtrack.support.PostgresTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class RbacIntegrationTest extends PostgresTestBase {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private BedRepository bedRepository;
    @Autowired private MedicineStockRepository medicineStockRepository;

    private Hospital hospital;
    private String doctorToken;
    private String receptionistToken;
    private String pharmacistToken;
    private String patientToken;
    private String nurseToken;
    private Long doctorId;
    private Long patientId;

    @BeforeEach
    void setUp() {
        hospital = hospitalRepository.save(Hospital.builder()
                .name("RBAC Hospital").licenseNumber("RBAC-" + System.nanoTime())
                .contactEmail("rbac@test.com")
                .subscriptionTier(SubscriptionTier.MONTHLY)
                .subscriptionStatus(SubscriptionStatus.ACTIVE).build());

        User docUser = userRepository.save(User.builder()
                .fullName("Dr. RBAC").email("doc-rbac-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.DOCTOR).hospital(hospital).build());

        User recUser = userRepository.save(User.builder()
                .fullName("Recep RBAC").email("recep-rbac-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.RECEPTIONIST).hospital(hospital).build());

        User pharmUser = userRepository.save(User.builder()
                .fullName("Pharm RBAC").email("pharm-rbac-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.PHARMACIST).hospital(hospital).build());

        User patUser = userRepository.save(User.builder()
                .fullName("Patient RBAC").email("pat-rbac-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.PATIENT).hospital(hospital).build());

        User nurseUser = userRepository.save(User.builder()
                .fullName("Nurse RBAC").email("nurse-rbac-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.NURSE).hospital(hospital).build());

        doctorToken = generateToken(docUser);
        receptionistToken = generateToken(recUser);
        pharmacistToken = generateToken(pharmUser);
        patientToken = generateToken(patUser);
        nurseToken = generateToken(nurseUser);

        Doctor doctor = doctorRepository.save(Doctor.builder()
                .hospital(hospital).user(docUser).specialization("General")
                .consultationFee(new BigDecimal("100")).isAvailable(true).build());
        doctorId = docUser.getId(); // AdmissionRequest.doctorId() expects User ID

        patientId = patientRepository.save(Patient.builder()
                .hospital(hospital).firstName("RBAC").lastName("Patient")
                .phoneNumber("555-RBAC-1").build()).getId();
    }

    private String generateToken(User user) {
        return jwtService.generateToken(new UserPrincipal(user),
                Map.of("role", user.getRole().name(), "userId", user.getId(),
                        "hospitalId", user.getHospital().getId()));
    }

    @Test
    void onlyDoctorOrAdmin_canCreateMedicalRecord() throws Exception {
        String payload = """
            {"patientId":%d,"encounterDate":"2026-07-24","chiefComplaints":"Cough"}
            """.formatted(patientId);

        mockMvc.perform(post("/api/medical-records")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/medical-records")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/medical-records")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    void onlyPharmacistOrAdmin_canDispense() throws Exception {
        Long stockId = medicineStockRepository.save(MedicineStock.builder()
                .hospital(hospital).medicineName("Test Med")
                .batchNumber("RBAC-BATCH").expiryDate(LocalDate.now().plusYears(1))
                .availableQuantity(100).unitPrice(new BigDecimal("10")).build()).getId();

        String payload = """
            {"patientId":%d,"medicineName":"Test Med","quantity":1}
            """.formatted(patientId);

        mockMvc.perform(post("/api/pharmacy/dispense")
                        .header("Authorization", "Bearer " + pharmacistToken)
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/pharmacy/dispense")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/pharmacy/dispense")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    void onlyDoctorOrAdmin_canAdmitPatient() throws Exception {
        Long bedId = bedRepository.save(Bed.builder()
                .hospital(hospital).wardName("RBAC Ward")
                .bedNumber("RBAC-1").chargePerDay(new BigDecimal("500"))
                .isOccupied(false).build()).getId();

        String payload = """
            {"patientId":%d,"doctorId":%d,"bedId":%d,"admissionDate":"2026-07-24"}
            """.formatted(patientId, doctorId, bedId);

        mockMvc.perform(post("/api/admissions")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admissions")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    void receptionist_canRegisterPatient() throws Exception {
        mockMvc.perform(post("/api/patients")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"firstName":"New","lastName":"Patient","phoneNumber":"555-RBAC-2"}
                        """))
                .andExpect(status().isOk());
    }

    @Test
    void patient_cannotRegisterOtherPatient() throws Exception {
        mockMvc.perform(post("/api/patients")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"firstName":"Hacker","lastName":"Patient","phoneNumber":"555-RBAC-3"}
                        """))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedUsers_canAccessHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/health")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk());
    }
}
