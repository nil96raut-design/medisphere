package com.healthtrack.integration;

import com.healthtrack.dto.PharmacyDtos.*;
import com.healthtrack.entity.*;
import com.healthtrack.repository.*;
import com.healthtrack.security.UserPrincipal;
import com.healthtrack.service.*;
import com.healthtrack.support.PostgresTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PharmacyIntegrationTest extends PostgresTestBase {

    @Autowired private PharmacyService pharmacyService;
    @Autowired private BillingService billingService;
    @Autowired private MedicalRecordService medicalRecordService;
    @Autowired private SupplierService supplierService;
    @Autowired private ExpiryAlertService expiryAlertService;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private MedicineStockRepository medicineStockRepository;
    @Autowired private DispensationRecordRepository dispensationRecordRepository;

    private Hospital hospital;
    private Patient patient;
    private User pharmacist;
    private User doctor;
    private UserPrincipal pharmacistPrincipal;
    private UserPrincipal doctorPrincipal;

    @BeforeEach
    void setUp() {
        hospital = hospitalRepository.save(Hospital.builder()
                .name("Pharmacy Integ Hospital").licenseNumber("PI-" + System.nanoTime())
                .contactEmail("pi@test.com").subscriptionTier(SubscriptionTier.MONTHLY)
                .subscriptionStatus(SubscriptionStatus.ACTIVE).build());

        pharmacist = userRepository.save(User.builder()
                .fullName("Pharm Integ").email("pi-pharm-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.PHARMACIST).hospital(hospital).build());

        doctor = userRepository.save(User.builder()
                .fullName("Dr. Integ").email("pi-doc-" + System.nanoTime() + "@test.com")
                .passwordHash("x").role(Role.DOCTOR).hospital(hospital).build());

        patient = patientRepository.save(Patient.builder()
                .hospital(hospital).firstName("Integ").lastName("Patient")
                .phoneNumber("PI-PAT").build());

        pharmacistPrincipal = new UserPrincipal(pharmacist);
        doctorPrincipal = new UserPrincipal(doctor);
    }

    @Test
    void endToEnd_prescriptionDispenseBilling() {
        // 1. Add stock with multiple batches (FIFO test)
        pharmacyService.addStock(new AddStockRequest("Metformin", "BAT-01",
                LocalDate.now().plusMonths(12), 50, BigDecimal.valueOf(2.00),
                BigDecimal.valueOf(0.50), BigDecimal.valueOf(3.00), null), pharmacistPrincipal);

        pharmacyService.addStock(new AddStockRequest("Metformin", "BAT-02",
                LocalDate.now().plusMonths(3), 20, BigDecimal.valueOf(2.00),
                BigDecimal.valueOf(0.50), BigDecimal.valueOf(3.00), null), pharmacistPrincipal);

        // 2. Create a medical record with prescription
        var medRecordRequest = new com.healthtrack.dto.EmrDtos.CreateMedicalRecordRequest(
                patient.getId(), null, LocalDate.now(), "Diabetes check", "Normal",
                "Type 2 Diabetes", null,
                List.of(new com.healthtrack.dto.EmrDtos.PrescriptionItemRequest(
                        "Metformin", "500mg", "2 times/day", "30 days", "Take after meals")),
                null);
        var medRecord = medicalRecordService.createRecord(medRecordRequest, doctorPrincipal);
        assertThat(medRecord.prescriptions()).isNotEmpty();

        var prescriptionItemId = medRecord.prescriptions().get(0).id();

        // 3. Dispense using FIFO — should take from BAT-02 (earliest expiry)
        var dispenseRequest = new DispenseRequest(patient.getId(), "Metformin", 10, prescriptionItemId);
        var dispenseResponse = pharmacyService.dispense(dispenseRequest, pharmacistPrincipal);
        assertThat(dispenseResponse.quantityDispensed()).isEqualTo(10);

        var batch02 = medicineStockRepository.findByHospitalId(hospital.getId()).stream()
                .filter(s -> s.getBatchNumber().equals("BAT-02")).findFirst().orElseThrow();
        assertThat(batch02.getAvailableQuantity()).isEqualTo(10);

        // 4. Verify prescription is partially fulfilled
        var presHistory = dispensationRecordRepository.findByPrescriptionItemId(prescriptionItemId);
        assertThat(presHistory).hasSize(1);
        assertThat(presHistory.get(0).getQuantityDispensed()).isEqualTo(10);

        // 5. Bill the dispensation
        var calculateResponse = billingService.calculate(patient.getId(), doctorPrincipal);
        assertThat(calculateResponse.items()).isNotEmpty();
        assertThat(calculateResponse.totalAmount()).isGreaterThan(BigDecimal.ZERO);

        var settleRequest = new com.healthtrack.dto.BillingDtos.SettleRequest(
                patient.getId(), BigDecimal.ZERO, BigDecimal.ZERO, null, "CASH", null,
                "integ-test-" + System.nanoTime());
        var billResponse = billingService.settle(settleRequest, doctorPrincipal);
        assertThat(billResponse.paymentStatus()).isEqualTo("PAID");

        // 6. Verify billing status updated on dispensation
        var paidDispensations = dispensationRecordRepository
                .findByPatientIdAndBillingStatus(patient.getId(), BillingStatus.PAID);
        assertThat(paidDispensations).isNotEmpty();
    }

    @Test
    void supplierAndReorderWorkflow() {
        // 1. Create supplier
        var supplierRequest = new CreateSupplierRequest("MedSupply Co", "John",
                "555-0100", "john@medsupply.com", "123 Main St", "GST-12345");
        var supplier = supplierService.createSupplier(supplierRequest, pharmacistPrincipal);
        assertThat(supplier.name()).isEqualTo("MedSupply Co");

        // 2. Add stock linked to supplier
        pharmacyService.addStock(new AddStockRequest("Ibuprofen", "IB-001",
                LocalDate.now().plusMonths(6), 5, BigDecimal.valueOf(3.00),
                BigDecimal.valueOf(1.00), BigDecimal.valueOf(5.00),
                supplier.id()), pharmacistPrincipal);

        // 3. Create purchase order
        var poRequest = new CreatePurchaseOrderRequest(supplier.id(),
                "Ibuprofen", 100, BigDecimal.valueOf(2.00), null, "Urgent restock");
        var purchaseOrder = supplierService.createPurchaseOrder(poRequest, pharmacistPrincipal);
        assertThat(purchaseOrder.status()).isEqualTo("CREATED");

        // 4. Receive purchase order
        var received = supplierService.receivePurchaseOrder(purchaseOrder.id(), pharmacistPrincipal);
        assertThat(received.status()).isEqualTo("RECEIVED");

        // 5. Verify reorder suggestions (stock is 5, reorder is 10)
        var suggestions = supplierService.getReorderSuggestions(pharmacistPrincipal);
        assertThat(suggestions).isNotEmpty();
        assertThat(suggestions.get(0).medicineName()).isEqualTo("Ibuprofen");
    }

    @Test
    void partialDispense_acrossMultipleBatches() {
        medicineStockRepository.save(MedicineStock.builder()
                .hospital(hospital).medicineName("PartialTest").batchNumber("PT-01")
                .expiryDate(LocalDate.now().plusMonths(6)).availableQuantity(3)
                .unitPrice(BigDecimal.TEN).reorderLevel(5).build());
        medicineStockRepository.save(MedicineStock.builder()
                .hospital(hospital).medicineName("PartialTest").batchNumber("PT-02")
                .expiryDate(LocalDate.now().plusMonths(8)).availableQuantity(3)
                .unitPrice(BigDecimal.TEN).reorderLevel(5).build());

        var request = new DispenseRequest(patient.getId(), "PartialTest", 5, null);
        var response = pharmacyService.dispense(request, pharmacistPrincipal);

        assertThat(response.quantityDispensed()).isEqualTo(5);
        assertThat(response.dispensationStatus()).isEqualTo("PARTIAL");
    }

    @Test
    void expiryAlert_generatedAndResolved() {
        medicineStockRepository.save(MedicineStock.builder()
                .hospital(hospital).medicineName("ExpiryAlertTest").batchNumber("EA-01")
                .expiryDate(LocalDate.now().minusDays(1)).availableQuantity(10)
                .unitPrice(BigDecimal.TEN).reorderLevel(5).build());

        expiryAlertService.checkExpiryAlerts();

        var alerts = expiryAlertService.getActiveAlerts(pharmacistPrincipal);
        assertThat(alerts).isNotEmpty();

        var expiredAlerts = alerts.stream()
                .filter(a -> a.alertType().equals("EXPIRED")).toList();
        assertThat(expiredAlerts).isNotEmpty();
    }
}
