package com.healthtrack.service;

import com.healthtrack.dto.PharmacyDtos.*;
import com.healthtrack.entity.*;
import com.healthtrack.repository.*;
import com.healthtrack.security.TenantValidator;
import com.healthtrack.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.healthtrack.repository.PatientRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PharmacyService {

    private final MedicineStockRepository medicineStockRepository;
    private final DispensationRecordRepository dispensationRecordRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final PatientRepository patientRepository;
    private final TenantValidator tenantValidator;

    @Transactional(readOnly = true)
    public List<PendingPrescriptionResponse> getPendingPrescriptions(UserPrincipal currentUser) {
        List<PrescriptionItem> items = prescriptionItemRepository.findPendingByHospital(currentUser.getHospitalId());
        return items.stream().map(p -> new PendingPrescriptionResponse(
                p.getId(),
                p.getMedicineName(),
                p.getDosage(),
                p.getFrequency(),
                p.getDuration(),
                p.getMedicalRecord().getPatient().getFirstName() + " " + p.getMedicalRecord().getPatient().getLastName(),
                p.getMedicalRecord().getDoctor().getFullName()
        )).toList();
    }

    @Transactional(readOnly = true)
    public List<MedicineStockResponse> getLowStock(UserPrincipal currentUser) {
        return medicineStockRepository.findByAvailableQuantityLessThanEqual(10).stream()
                .map(this::mapStock).toList();
    }

    @Transactional(readOnly = true)
    public List<MedicineStockResponse> getAllStock(UserPrincipal currentUser) {
        return medicineStockRepository.findAll().stream()
                .map(this::mapStock).toList();
    }

    @Transactional
    public MedicineStockResponse addStock(AddStockRequest request, UserPrincipal currentUser) {
        Hospital hospital = currentUser.getUser().getHospital();

        MedicineStock stock = MedicineStock.builder()
                .hospital(hospital)
                .medicineName(request.medicineName())
                .batchNumber(request.batchNumber())
                .expiryDate(request.expiryDate())
                .availableQuantity(request.quantity())
                .unitPrice(request.unitPrice() != null ? request.unitPrice() : BigDecimal.ZERO)
                .build();

        stock = medicineStockRepository.save(stock);
        return mapStock(stock);
    }

    @Transactional
    public DispensationResponse dispense(DispenseRequest request, UserPrincipal currentUser) {
        User pharmacist = currentUser.getUser();
        if (pharmacist.getRole() != Role.PHARMACIST && pharmacist.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only pharmacists can dispense medication");
        }

        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
        tenantValidator.validateHospitalAccess(patient.getHospital().getId(), currentUser.getHospitalId());

        MedicineStock stock = medicineStockRepository.findByIdLocked(request.medicineStockId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Medicine not found in stock"));

        if (stock.getExpiryDate().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Medicine '" + stock.getMedicineName() + "' batch has expired");
        }

        if (stock.getAvailableQuantity() < request.quantity()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Insufficient stock. Available: " + stock.getAvailableQuantity()
                            + ", requested: " + request.quantity());
        }

        stock.setAvailableQuantity(stock.getAvailableQuantity() - request.quantity());
        medicineStockRepository.save(stock);

        BigDecimal totalPrice = stock.getUnitPrice().multiply(BigDecimal.valueOf(request.quantity()));

        DispensationRecord record = DispensationRecord.builder()
                .hospital(stock.getHospital())
                .patient(patient)
                .medicineStock(stock)
                .medicineName(stock.getMedicineName())
                .quantityDispensed(request.quantity())
                .unitPrice(stock.getUnitPrice())
                .totalPrice(totalPrice)
                .billingStatus(BillingStatus.PENDING)
                .dispensedBy(pharmacist)
                .build();

        if (request.prescriptionItemId() != null) {
            PrescriptionItem item = prescriptionItemRepository.findById(request.prescriptionItemId())
                    .orElse(null);
            record.setPrescriptionItem(item);
        }

        record = dispensationRecordRepository.save(record);
        return mapDispensation(record);
    }

    private MedicineStockResponse mapStock(MedicineStock s) {
        return new MedicineStockResponse(
                s.getId(), s.getMedicineName(), s.getBatchNumber(),
                s.getExpiryDate(), s.getAvailableQuantity(),
                s.getReorderLevel(), s.getUnitPrice(),
                s.getExpiryDate().isBefore(LocalDate.now()),
                s.getAvailableQuantity() <= s.getReorderLevel());
    }

    private DispensationResponse mapDispensation(DispensationRecord r) {
        return new DispensationResponse(
                r.getId(), r.getMedicineName(), r.getQuantityDispensed(),
                r.getTotalPrice(), r.getBillingStatus().name(),
                r.getDispensedAt(), r.getDispensedBy().getFullName());
    }
}
