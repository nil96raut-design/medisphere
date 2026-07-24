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
import java.time.LocalTime;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class TenantIsolationIntegrationTest extends PostgresTestBase {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private TaskRepository taskRepository;

    private String tokenA;
    private String tokenB;
    private Long patientAId;
    private Long taskAId;
    private User userA;

    @BeforeEach
    void setUp() {
        Hospital hospitalA = hospitalRepository.save(Hospital.builder()
                .name("Hospital Alpha").licenseNumber("ALPHA-" + System.nanoTime())
                .contactEmail("alpha@test.com")
                .subscriptionTier(SubscriptionTier.MONTHLY)
                .subscriptionStatus(SubscriptionStatus.ACTIVE).build());

        Hospital hospitalB = hospitalRepository.save(Hospital.builder()
                .name("Hospital Beta").licenseNumber("BETA-" + System.nanoTime())
                .contactEmail("beta@test.com")
                .subscriptionTier(SubscriptionTier.MONTHLY)
                .subscriptionStatus(SubscriptionStatus.ACTIVE).build());

        User userA = userRepository.save(User.builder()
                .fullName("User Alpha").email("user-a-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.DOCTOR).hospital(hospitalA).build());

        User userB = userRepository.save(User.builder()
                .fullName("User Beta").email("user-b-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.DOCTOR).hospital(hospitalB).build());

        tokenA = jwtService.generateToken(new UserPrincipal(userA),
                Map.of("role", "DOCTOR", "userId", userA.getId(), "hospitalId", userA.getHospital().getId()));
        tokenB = jwtService.generateToken(new UserPrincipal(userB),
                Map.of("role", "DOCTOR", "userId", userB.getId(), "hospitalId", userB.getHospital().getId()));

        Doctor doctorA = doctorRepository.save(Doctor.builder()
                .hospital(hospitalA).user(userA).specialization("Cardio")
                .consultationFee(new BigDecimal("100")).isAvailable(true).build());

        Doctor doctorB = doctorRepository.save(Doctor.builder()
                .hospital(hospitalB).user(userB).specialization("Derma")
                .consultationFee(new BigDecimal("80")).isAvailable(true).build());

        patientAId = patientRepository.save(Patient.builder()
                .hospital(hospitalA).firstName("AlphaPat").lastName("One")
                .phoneNumber("555-ALPHA-1").build()).getId();

        patientRepository.save(Patient.builder()
                .hospital(hospitalB).firstName("BetaPat").lastName("One")
                .phoneNumber("555-BETA-1").build()).getId();

        taskAId = taskRepository.save(Task.builder()
                .hospital(hospitalA).title("Alpha Task")
                .description("Task belonging to Hospital A")
                .assignee(userA).assignedBy(userA)
                .priority(TaskPriority.MEDIUM)
                .dueDate(LocalDate.now().plusDays(7))
                .status(TaskStatus.NOT_STARTED)
                .progressPercent(0)
                .build()).getId();
    }

    @Test
    void userFromHospitalA_seesOnlyOwnPatients() throws Exception {
        mockMvc.perform(get("/api/patients/search")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("q", "AlphaPat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].firstName").value("AlphaPat"));

        mockMvc.perform(get("/api/patients/search")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("q", "BetaPat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void userFromHospitalB_cannotSeeHospitalADoctors() throws Exception {
        mockMvc.perform(get("/api/doctors/available")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.specialization == 'Cardio')]").doesNotExist());
    }

    @Test
    void userFromHospitalB_cannotUpdateHospitalATask() throws Exception {
        // userB tries to update a task that belongs to Hospital A → 403
        mockMvc.perform(patch("/api/tasks/" + taskAId + "/progress")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"status":"IN_PROGRESS","progressPercent":50,"note":"Cross-tenant attempt"}
                        """))
                .andExpect(status().isForbidden());
    }

    @Test
    void userFromHospitalB_cannotViewHospitalATaskTimeline() throws Exception {
        mockMvc.perform(get("/api/tasks/" + taskAId + "/timeline")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
    }
}
