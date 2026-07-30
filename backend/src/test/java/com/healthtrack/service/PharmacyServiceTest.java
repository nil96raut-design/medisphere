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
    @Autowired private MedicineStockRepository medicineStockRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private UserRepository userRepository;

    private Hospital hospital;
    private User pharmacist;
    private Patient patient;
    private UserPrincipal pharmacistPrincipal;

    @BeforeEach
    void setUp() {
        hospital = hospitalRepository.save(Hospital.builder()
                .name("Pharmacy Test Hospital").licenseNumber("PT-" + System.nanoTime())
                .contactEmail("pt@test.com").subscriptionTier(SubscriptionTier.MONTHLY)
                .subscriptionStatus(SubscriptionStatus.ACTIVE).build());

        pharmacist = userRepository.save(User.builder()
                .fullName("Pharm Test").email("pharm-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.PHARMACIST).hospital(hospital).build());

        patient = patientRepository.save(Patient.builder()
                .hospital(hospital).firstName("Pharm").lastName("Patient")
                .phoneNumber("PT-PAT").build());

        pharmacistPrincipal = new UserPrincipal(pharmacist);
    }

    @Test
    void addStock_createsStock() {
        var request = new AddStockRequest("Paracetamol", "BATCH-001",
                LocalDate.now().plusMonths(6), 100, BigDecimal.valueOf(5.00),
                BigDecimal.valueOf(2.00), BigDecimal.valueOf(7.00), null);
        var response = pharmacyService.addStock(request, pharmacistPrincipal);
        assertThat(response.medicineName()).isEqualTo("Paracetamol");
        assertThat(response.availableQuantity()).isEqualTo(100);
        assertThat(response.effectiveQuantity()).isEqualTo(100);
    }

    @Test
    void fifo_selectsEarliestExpiryFirst() {
        medicineStockRepository.save(MedicineStock.builder()
                .hospital(hospital).medicineName("Amoxicillin").batchNumber("BATCH-A")
                .expiryDate(LocalDate.now().plusMonths(6)).availableQuantity(50)
                .unitPrice(BigDecimal.TEN).reorderLevel(10).build());
        medicineStockRepository.save(MedicineStock.builder()
                .hospital(hospital).medicineName("Amoxicillin").batchNumber("BATCH-B")
                .expiryDate(LocalDate.now().plusMonths(2)).availableQuantity(30)
                .unitPrice(BigDecimal.TEN).reorderLevel(10).build());
        medicineStockRepository.save(MedicineStock.builder()
                .hospital(hospital).medicineName("Amoxicillin").batchNumber("BATCH-C")
                .expiryDate(LocalDate.now().plusMonths(12)).availableQuantity(20)
                .unitPrice(BigDecimal.TEN).reorderLevel(10).build());

        var request = new DispenseRequest(patient.getId(), "Amoxicillin", 25, null);
        var response = pharmacyService.dispense(request, pharmacistPrincipal);

        assertThat(response.dispensationStatus()).isEqualTo("COMPLETE");
        assertThat(response.quantityDispensed()).isEqualTo(25);

        var batchA = medicineStockRepository.findByHospitalId(hospital.getId()).stream()
                .filter(s -> s.getBatchNumber().equals("BATCH-A")).findFirst().orElseThrow();
        var batchB = medicineStockRepository.findByHospitalId(hospital.getId()).stream()
                .filter(s -> s.getBatchNumber().equals("BATCH-B")).findFirst().orElseThrow();
        var batchC = medicineStockRepository.findByHospitalId(hospital.getId()).stream()
                .filter(s -> s.getBatchNumber().equals("BATCH-C")).findFirst().orElseThrow();

        assertThat(batchB.getAvailableQuantity()).isEqualTo(5);
        assertThat(batchA.getAvailableQuantity()).isEqualTo(50);
        assertThat(batchC.getAvailableQuantity()).isEqualTo(20);
    }

    @Test
    void expiredBatch_isBlocked() {
        medicineStockRepository.save(MedicineStock.builder()
                .hospital(hospital).medicineName("ExpiredMed").batchNumber("EXP-001")
                .expiryDate(LocalDate.now().minusDays(1)).availableQuantity(50)
                .unitPrice(BigDecimal.TEN).reorderLevel(10).build());
        medicineStockRepository.save(MedicineStock.builder()
                .hospital(hospital).medicineName("ExpiredMed").batchNumber("EXP-002")
                .expiryDate(LocalDate.now().plusMonths(6)).availableQuantity(10)
                .unitPrice(BigDecimal.TEN).reorderLevel(10).build());

        var request = new DispenseRequest(patient.getId(), "ExpiredMed", 5, null);
        var response = pharmacyService.dispense(request, pharmacistPrincipal);
        assertThat(response.quantityDispensed()).isEqualTo(5);

        var expiredBatch = medicineStockRepository.findByHospitalId(hospital.getId()).stream()
                .filter(s -> s.getBatchNumber().equals("EXP-001")).findFirst().orElseThrow();
        assertThat(expiredBatch.getAvailableQuantity()).isEqualTo(50);
    }

    @Test
    void insufficientStock_throws() {
        medicineStockRepository.save(MedicineStock.builder()
                .hospital(hospital).medicineName("LowStockMed").batchNumber("LOW-001")
                .expiryDate(LocalDate.now().plusMonths(6)).availableQuantity(3)
                .unitPrice(BigDecimal.TEN).reorderLevel(10).build());

        var request = new DispenseRequest(patient.getId(), "LowStockMed", 10, null);
        assertThatThrownBy(() -> pharmacyService.dispense(request, pharmacistPrincipal))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void partialDispensing_multiBatch() {
        medicineStockRepository.save(MedicineStock.builder()
                .hospital(hospital).medicineName("MultiBatch").batchNumber("MB-001")
                .expiryDate(LocalDate.now().plusMonths(6)).availableQuantity(5)
                .unitPrice(BigDecimal.TEN).reorderLevel(10).build());
        medicineStockRepository.save(MedicineStock.builder()
                .hospital(hospital).medicineName("MultiBatch").batchNumber("MB-002")
                .expiryDate(LocalDate.now().plusMonths(8)).availableQuantity(5)
                .unitPrice(BigDecimal.TEN).reorderLevel(10).build());

        var request = new DispenseRequest(patient.getId(), "MultiBatch", 8, null);
        var response = pharmacyService.dispense(request, pharmacistPrincipal);

        assertThat(response.quantityDispensed()).isEqualTo(8);
        assertThat(response.dispensationStatus()).isEqualTo("PARTIAL");
    }

    @Test
    void noValidBatches_throws() {
        var request = new DispenseRequest(patient.getId(), "NonExistentMed", 5, null);
        assertThatThrownBy(() -> pharmacyService.dispense(request, pharmacistPrincipal))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void nonPharmacist_throws() {
        User doctor = userRepository.save(User.builder()
                .fullName("Dr. No").email("dr-no-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.DOCTOR).hospital(hospital).build());
        var request = new DispenseRequest(patient.getId(), "Anything", 1, null);
        assertThatThrownBy(() -> pharmacyService.dispense(request, new UserPrincipal(doctor)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void lowStock_detected() {
        medicineStockRepository.save(MedicineStock.builder()
                .hospital(hospital).medicineName("LowItem").batchNumber("LOW-001")
                .expiryDate(LocalDate.now().plusMonths(6)).availableQuantity(5)
                .unitPrice(BigDecimal.TEN).reorderLevel(10).build());

        var lowStock = pharmacyService.getLowStock(pharmacistPrincipal);
        assertThat(lowStock).isNotEmpty();
        assertThat(lowStock.get(0).medicineName()).isEqualTo("LowItem");
    }
}
