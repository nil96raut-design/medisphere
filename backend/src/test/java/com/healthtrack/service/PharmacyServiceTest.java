package com.healthtrack.service;

import com.healthtrack.dto.PharmacyDtos.*;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PharmacyServiceTest extends PostgresTestBase {

    @Autowired private PharmacyService pharmacyService;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private MedicineStockRepository medicineStockRepository;

    private Hospital hospital;
    private Patient patient;
    private UserPrincipal pharmacistPrincipal;
    private UserPrincipal adminPrincipal;
    private UserPrincipal patientPrincipal;

    @BeforeEach
    void setUp() {
        hospital = hospitalRepository.save(Hospital.builder()
                .name("Pharmacy Test Hospital").licenseNumber("PHARM-" + System.nanoTime())
                .contactEmail("pharm@test.com")
                .subscriptionTier(SubscriptionTier.MONTHLY)
                .subscriptionStatus(SubscriptionStatus.ACTIVE).build());

        User pharmacist = userRepository.save(User.builder()
                .fullName("Pharmacist Alex").email("pharmacist-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.PHARMACIST).hospital(hospital).build());

        User admin = userRepository.save(User.builder()
                .fullName("Admin User").email("admin-pharm-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.ADMIN).hospital(hospital).build());

        User patUser = userRepository.save(User.builder()
                .fullName("Pharmacy Patient").email("pat-pharm-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.PATIENT).hospital(hospital).build());

        pharmacistPrincipal = new UserPrincipal(pharmacist);
        adminPrincipal = new UserPrincipal(admin);
        patientPrincipal = new UserPrincipal(patUser);

        patient = patientRepository.save(Patient.builder()
                .hospital(hospital).firstName("Pharma").lastName("Patient")
                .phoneNumber("555-PHARM-1").build());
    }

    @Test
    void addStock_createsMedicine() {
        MedicineStockResponse stock = pharmacyService.addStock(
                new AddStockRequest("Paracetamol", "BATCH-001", LocalDate.now().plusYears(2), 100, new BigDecimal("5.00")),
                pharmacistPrincipal);

        assertThat(stock.medicineName()).isEqualTo("Paracetamol");
        assertThat(stock.availableQuantity()).isEqualTo(100);
        assertThat(stock.isExpired()).isFalse();
    }

    @Test
    void dispense_reducesStock() {
        MedicineStockResponse stock = pharmacyService.addStock(
                new AddStockRequest("Amoxicillin", "BATCH-002", LocalDate.now().plusYears(1), 50, new BigDecimal("10.00")),
                pharmacistPrincipal);

        DispensationResponse dispensation = pharmacyService.dispense(
                new DispenseRequest(patient.getId(), stock.id(), 10, null),
                pharmacistPrincipal);

        assertThat(dispensation.quantityDispensed()).isEqualTo(10);
        assertThat(dispensation.billingStatus()).isEqualTo("PENDING");

        MedicineStockResponse updated = medicineStockRepository.findById(stock.id())
                .map(s -> new MedicineStockResponse(s.getId(), s.getMedicineName(), s.getBatchNumber(),
                        s.getExpiryDate(), s.getAvailableQuantity(), s.getReorderLevel(),
                        s.getUnitPrice(), false, false))
                .orElseThrow();
        assertThat(updated.availableQuantity()).isEqualTo(40);
    }

    @Test
    void dispense_insufficientStock_throws400() {
        MedicineStockResponse stock = pharmacyService.addStock(
                new AddStockRequest("Ibuprofen", "BATCH-003", LocalDate.now().plusYears(1), 5, new BigDecimal("3.00")),
                pharmacistPrincipal);

        assertThatThrownBy(() -> pharmacyService.dispense(
                new DispenseRequest(patient.getId(), stock.id(), 10, null),
                pharmacistPrincipal))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void dispense_expiredMedicine_throws400() {
        MedicineStockResponse stock = pharmacyService.addStock(
                new AddStockRequest("Expired Med", "BATCH-EXP", LocalDate.now().minusDays(1), 50, new BigDecimal("1.00")),
                pharmacistPrincipal);

        assertThatThrownBy(() -> pharmacyService.dispense(
                new DispenseRequest(patient.getId(), stock.id(), 1, null),
                pharmacistPrincipal))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void dispense_nonPharmacist_throws403() {
        MedicineStockResponse stock = pharmacyService.addStock(
                new AddStockRequest("Test Med", "BATCH-004", LocalDate.now().plusYears(1), 50, new BigDecimal("2.00")),
                pharmacistPrincipal);

        assertThatThrownBy(() -> pharmacyService.dispense(
                new DispenseRequest(patient.getId(), stock.id(), 1, null),
                patientPrincipal))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
    }

    @Test
    void getLowStock_filtersByThreshold() {
        pharmacyService.addStock(
                new AddStockRequest("Low Stock Med", "BATCH-LOW", LocalDate.now().plusYears(1), 5, new BigDecimal("1.00")),
                pharmacistPrincipal);
        pharmacyService.addStock(
                new AddStockRequest("Full Stock Med", "BATCH-FULL", LocalDate.now().plusYears(1), 100, new BigDecimal("1.00")),
                pharmacistPrincipal);

        var lowStock = pharmacyService.getLowStock(pharmacistPrincipal);
        assertThat(lowStock).anyMatch(s -> s.medicineName().equals("Low Stock Med"));
        assertThat(lowStock).noneMatch(s -> s.medicineName().equals("Full Stock Med"));
    }

    @Test
    void getAllStock_returnsAll() {
        pharmacyService.addStock(
                new AddStockRequest("Med A", "BATCH-A", LocalDate.now().plusYears(1), 10, null),
                pharmacistPrincipal);
        pharmacyService.addStock(
                new AddStockRequest("Med B", "BATCH-B", LocalDate.now().plusYears(1), 20, null),
                pharmacistPrincipal);

        assertThat(pharmacyService.getAllStock(pharmacistPrincipal)).hasSize(2);
    }

    @Test
    void admin_canDispense() {
        MedicineStockResponse stock = pharmacyService.addStock(
                new AddStockRequest("Admin Dispense", "BATCH-ADM", LocalDate.now().plusYears(1), 30, new BigDecimal("5.00")),
                pharmacistPrincipal);

        DispensationResponse result = pharmacyService.dispense(
                new DispenseRequest(patient.getId(), stock.id(), 5, null),
                adminPrincipal);

        assertThat(result.quantityDispensed()).isEqualTo(5);
    }
}
