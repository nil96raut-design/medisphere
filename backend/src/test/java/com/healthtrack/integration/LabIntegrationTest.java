package com.healthtrack.integration;

import com.healthtrack.dto.LabDtos.*;
import com.healthtrack.entity.*;
import com.healthtrack.repository.*;
import com.healthtrack.security.UserPrincipal;
import com.healthtrack.support.PostgresTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LabIntegrationTest extends PostgresTestBase {

    @Autowired private WebApplicationContext context;
    @Autowired private LabTestOrderRepository labTestOrderRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private UserRepository userRepository;

    private MockMvc mockMvc;
    private Hospital hospital;
    private User labTech;
    private Patient patient;
    private UserPrincipal techPrincipal;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        hospital = hospitalRepository.save(Hospital.builder()
                .name("Lab Int Test Hospital").licenseNumber("LIT-" + System.nanoTime())
                .contactEmail("lit@test.com").subscriptionTier(SubscriptionTier.MONTHLY)
                .subscriptionStatus(SubscriptionStatus.ACTIVE).build());

        labTech = userRepository.save(User.builder()
                .fullName("Lab Tech Int").email("labtech-int-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.LAB_TECH).hospital(hospital).build());

        patient = patientRepository.save(Patient.builder()
                .hospital(hospital).firstName("LabInt").lastName("Patient")
                .phoneNumber("LIT-PAT").build());

        techPrincipal = new UserPrincipal(labTech);
    }

    @Test
    void getLabQueue_returnsOk() throws Exception {
        mockMvc.perform(get("/api/lab/queue").with(user(techPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingCollection").isArray())
                .andExpect(jsonPath("$.metrics").exists());
    }

    @Test
    void getLabMetrics_returnsData() throws Exception {
        mockMvc.perform(get("/api/lab/metrics").with(user(techPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingCollection").exists())
                .andExpect(jsonPath("$.totalOrdersToday").exists());
    }

    @Test
    void approveOrder_doctorOnly() throws Exception {
        LabTestOrder order = labTestOrderRepository.save(LabTestOrder.builder()
                .hospital(hospital).patient(patient).testName("Approve Test")
                .requestedBy(labTech).build());

        mockMvc.perform(put("/api/lab/orders/" + order.getId() + "/sample")
                        .with(user(techPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"technicianNotes\":\"collected\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/lab/orders/" + order.getId() + "/process")
                        .with(user(techPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/lab/orders/" + order.getId() + "/results")
                        .with(user(techPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resultValues\":\"Normal\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/lab/orders/" + order.getId() + "/approve")
                        .with(user(techPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void criticalRulesCrud() throws Exception {
        mockMvc.perform(post("/api/lab/critical-rules")
                        .with(user(techPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"testName\":\"CBC\",\"parameterName\":\"Hemoglobin\",\"conditionOperator\":\"<\",\"thresholdValue\":\"10\",\"unit\":\"g/dL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());

        mockMvc.perform(get("/api/lab/critical-rules").with(user(techPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].testName").value("CBC"));
    }

    @Test
    void fullLabOrderLifecycle() throws Exception {
        LabTestOrder order = labTestOrderRepository.save(LabTestOrder.builder()
                .hospital(hospital).patient(patient).testName("Lifecycle Test")
                .requestedBy(labTech).build());

        mockMvc.perform(get("/api/lab/orders/" + order.getId()).with(user(techPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ORDERED"));

        mockMvc.perform(put("/api/lab/orders/" + order.getId() + "/sample")
                        .with(user(techPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"technicianNotes\":\"Sample taken\",\"sampleType\":\"Blood\",\"barcode\":\"BAR-100\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SAMPLE_COLLECTED"));

        mockMvc.perform(put("/api/lab/orders/" + order.getId() + "/process")
                        .with(user(techPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"technicianNotes\":\"Processing started\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"));

        mockMvc.perform(put("/api/lab/orders/" + order.getId() + "/results")
                        .with(user(techPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resultValues\":\"Glucose: 95 mg/dL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESULT_ENTERED"));

        mockMvc.perform(put("/api/lab/orders/" + order.getId() + "/cancel")
                        .with(user(techPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
