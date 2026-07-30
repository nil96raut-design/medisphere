package com.healthtrack.service;

import com.healthtrack.dto.PharmacyDtos.*;
import com.healthtrack.entity.*;
import com.healthtrack.event.EventConstants;
import com.healthtrack.event.EventPublisher;
import com.healthtrack.repository.*;
import com.healthtrack.security.TenantValidator;
import com.healthtrack.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PharmacyService {

    private static final Logger log = LoggerFactory.getLogger(PharmacyService.class);

    private final MedicineStockRepository medicineStockRepository;
    private final DispensationRecordRepository dispensationRecordRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final PatientRepository patientRepository;
    private final PharmacyRecallRepository pharmacyRecallRepository;
    private final TenantValidator tenantValidator;
    private final EventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<PendingPrescriptionItemResponse> getPendingPrescriptions(UserPrincipal currentUser) {
        List<PrescriptionItem> items = prescriptionItemRepository
                .findPendingByHospital(currentUser.getHospitalId());
        return items.stream().map(p -> {
            int alreadyDispensed = dispensationRecordRepository
                    .sumQuantityDispensedByPrescriptionItemId(p.getId());
            return new PendingPrescriptionItemResponse(
                    p.getId(),
                    p.getMedicineName(),
                    p.getDosage(),
                    p.getFrequency(),
                    p.getDuration(),
                    p.getInstructions(),
                    p.getMedicalRecord().getPatient().getFirstName() + " "
                            + p.getMedicalRecord().getPatient().getLastName(),
                    p.getMedicalRecord().getPatient().getId(),
                    p.getMedicalRecord().getDoctor().getFullName(),
                    alreadyDispensed,
                    0);
        }).toList();
    }

    @Cacheable(value = "inventoryCache", key = "#currentUser.hospitalId")
    @Transactional(readOnly = true)
    public List<MedicineStockResponse> getAllStock(UserPrincipal currentUser) {
        return medicineStockRepository.findByHospitalId(currentUser.getHospitalId())
                .stream().map(this::mapStock).toList();
    }

    @Transactional(readOnly = true)
    public List<MedicineStockResponse> getLowStock(UserPrincipal currentUser) {
        return medicineStockRepository.findLowStockByHospitalId(currentUser.getHospitalId())
                .stream().filter(s -> !s.isExpired()).map(this::mapStock).toList();
    }

    @Transactional(readOnly = true)
    public Page<DispensationRecord> getDispensationHistory(Long hospitalId, int page, int size) {
        return dispensationRecordRepository.findByHospitalId(hospitalId, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public List<DispensationRecord> getPatientDispensations(Long patientId) {
        return dispensationRecordRepository.findByPatientIdOrderByDispensedAtDesc(patientId);
    }

    @Transactional
    @CacheEvict(value = "inventoryCache", key = "#currentUser.hospitalId")
    public MedicineStockResponse addStock(AddStockRequest request, UserPrincipal currentUser) {
        Hospital hospital = currentUser.getUser().getHospital();

        Supplier supplier = null;
        if (request.supplierId() != null) {
            supplier = new Supplier();
            supplier.setId(request.supplierId());
        }

        BigDecimal unitPrice = request.unitPrice() != null ? request.unitPrice() : BigDecimal.ZERO;
        BigDecimal purchasePrice = request.purchasePrice() != null ? request.purchasePrice() : BigDecimal.ZERO;
        BigDecimal sellingPrice = request.sellingPrice() != null ? request.sellingPrice() : BigDecimal.ZERO;

        MedicineStock stock = MedicineStock.builder()
                .hospital(hospital)
                .medicineName(request.medicineName())
                .batchNumber(request.batchNumber())
                .expiryDate(request.expiryDate())
                .availableQuantity(request.quantity())
                .unitPrice(unitPrice)
                .purchasePrice(purchasePrice)
                .sellingPrice(sellingPrice)
                .supplier(supplier)
                .build();

        stock = medicineStockRepository.save(stock);
        return mapStock(stock);
    }

    @Transactional
    @CacheEvict(value = "inventoryCache", key = "#currentUser.hospitalId")
    public MedicineStockResponse updateStockPrices(Long stockId, UpdateStockPriceRequest request,
                                                    UserPrincipal currentUser) {
        MedicineStock stock = medicineStockRepository.findByIdLocked(stockId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock not found"));
        if (request.unitPrice() != null) stock.setUnitPrice(request.unitPrice());
        if (request.sellingPrice() != null) stock.setSellingPrice(request.sellingPrice());
        if (request.purchasePrice() != null) stock.setPurchasePrice(request.purchasePrice());
        if (request.reorderLevel() != null) stock.setReorderLevel(request.reorderLevel());
        stock = medicineStockRepository.save(stock);
        return mapStock(stock);
    }

    @Transactional(timeout = 5)
    @org.springframework.retry.annotation.Retryable(
            retryFor = {org.springframework.dao.CannotAcquireLockException.class, org.springframework.dao.PessimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @org.springframework.retry.annotation.Backoff(delay = 100, maxDelay = 500)
    )
    @CacheEvict(value = "inventoryCache", key = "#currentUser.hospitalId")
    public DispensationResponse dispense(DispenseRequest request, UserPrincipal currentUser) {
        User pharmacist = currentUser.getUser();
        if (pharmacist.getRole() != Role.PHARMACIST && pharmacist.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only pharmacists can dispense medication");
        }

        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
        tenantValidator.validateHospitalAccess(patient.getHospital().getId(), currentUser.getHospitalId());

        Long hospitalId = currentUser.getHospitalId();

        PrescriptionItem prescriptionItem = null;
        if (request.prescriptionItemId() != null) {
            prescriptionItem = prescriptionItemRepository.findById(request.prescriptionItemId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Prescription item not found"));

            int alreadyDispensed = dispensationRecordRepository
                    .sumQuantityDispensedByPrescriptionItemId(prescriptionItem.getId());

            int prescribedQty = parsePrescribedQuantity(prescriptionItem);
            if (alreadyDispensed >= prescribedQty) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Prescription item already fully dispensed (" + alreadyDispensed
                                + "/" + prescribedQty + ")");
            }
            if (request.quantity() + alreadyDispensed > prescribedQty) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Total dispensed would exceed prescribed quantity. Already dispensed: "
                                + alreadyDispensed + ", prescribed: " + prescribedQty
                                + ", requested: " + request.quantity());
            }
        }

        List<MedicineStock> validBatches = medicineStockRepository
                .findValidBatchesFifoLocked(hospitalId, request.medicineName());

        if (validBatches.isEmpty()) {
            eventPublisher.publish(EventConstants.EXPIRED_BLOCKED, hospitalId,
                    Map.of("medicineName", request.medicineName(), "reason", "No valid batches available"));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No valid (non-expired, in-stock) batches found for: " + request.medicineName());
        }

        int remainingToDispense = request.quantity();
        List<DispenseBatchResponse> batchResponses = new ArrayList<>();
        List<MedicineStock> updatedStocks = new ArrayList<>();
        DispensationStatus dispStatus = DispensationStatus.COMPLETE;
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (MedicineStock batch : validBatches) {
            if (remainingToDispense <= 0) break;

            int available = batch.getEffectiveQuantity();
            int take = Math.min(available, remainingToDispense);

            batch.setAvailableQuantity(batch.getAvailableQuantity() - take);
            updatedStocks.add(medicineStockRepository.save(batch));

            BigDecimal price = batch.getSellingPrice().compareTo(BigDecimal.ZERO) > 0
                    ? batch.getSellingPrice() : batch.getUnitPrice();
            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(take));
            totalPrice = totalPrice.add(subtotal);

            batchResponses.add(new DispenseBatchResponse(
                    batch.getId(), batch.getBatchNumber(), batch.getExpiryDate(),
                    take, price, subtotal));

            remainingToDispense -= take;
        }

        if (remainingToDispense > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Insufficient stock across all batches. Short by: " + remainingToDispense
                            + ", available total: " + (request.quantity() - remainingToDispense));
        }

        if (batchResponses.size() > 1) {
            dispStatus = DispensationStatus.PARTIAL;
        }

        BigDecimal unitPrice = batchResponses.size() == 1
                ? batchResponses.get(0).unitPrice() : totalPrice.divide(BigDecimal.valueOf(request.quantity()),
                BigDecimal.ROUND_HALF_UP);

        DispensationRecord record = DispensationRecord.builder()
                .hospital(patient.getHospital())
                .patient(patient)
                .prescriptionItem(prescriptionItem)
                .medicineStock(updatedStocks.get(0))
                .medicineName(request.medicineName())
                .quantityDispensed(request.quantity())
                .remainingQuantity(prescriptionItem != null
                        ? Math.max(0, parsePrescribedQuantity(prescriptionItem)
                                - dispensationRecordRepository.sumQuantityDispensedByPrescriptionItemId(
                                        prescriptionItem.getId()) - request.quantity())
                        : 0)
                .unitPrice(unitPrice)
                .totalPrice(totalPrice)
                .dispensationStatus(dispStatus)
                .batchId(updatedStocks.get(0).getId())
                .billingStatus(BillingStatus.PENDING)
                .dispensedBy(pharmacist)
                .build();

        if (prescriptionItem != null) {
            record.setPrescriptionItem(prescriptionItem);
        }

        record = dispensationRecordRepository.save(record);

        eventPublisher.publish(EventConstants.DISPENSE_MEDICINE, hospitalId,
                Map.of("dispensationId", record.getId(), "patientId", patient.getId(),
                        "medicineName", request.medicineName(), "quantity", request.quantity(),
                        "totalPrice", totalPrice.toString(),
                        "status", dispStatus.name()));

        if (dispStatus == DispensationStatus.PARTIAL) {
            eventPublisher.publish(EventConstants.PARTIAL_DISPENSE, hospitalId,
                    Map.of("dispensationId", record.getId(), "medicineName", request.medicineName(),
                            "dispensed", String.valueOf(request.quantity()),
                            "batches", String.valueOf(batchResponses.size())));
        }

        return mapDispensation(record, batchResponses);
    }

    @Transactional(readOnly = true)
    public List<MedicineStockResponse> getInventoryByMedicine(UserPrincipal currentUser, String medicineName) {
        return medicineStockRepository.findByHospitalId(currentUser.getHospitalId())
                .stream()
                .filter(s -> s.getMedicineName().equalsIgnoreCase(medicineName))
                .map(this::mapStock)
                .toList();
    }

    @Transactional
    public PharmacyRecallResponse flagRecall(RecallBatchRequest request, UserPrincipal currentUser) {
        if (pharmacyRecallRepository.findByMedicineNameAndBatchNumberAndActiveTrue(
                request.medicineName(), request.batchNumber()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Batch is already recalled");
        }

        PharmacyRecall recall = PharmacyRecall.builder()
                .hospital(currentUser.getUser().getHospital())
                .medicineName(request.medicineName())
                .batchNumber(request.batchNumber())
                .recallReason(request.recallReason())
                .build();

        recall = pharmacyRecallRepository.save(recall);

        eventPublisher.publish(EventConstants.EXPIRED_BLOCKED, currentUser.getHospitalId(),
                Map.of("medicineName", request.medicineName(),
                        "batchNumber", request.batchNumber(),
                        "reason", "BATCH_RECALLED: " + request.recallReason()));

        return new PharmacyRecallResponse(
                recall.getId(),
                recall.getMedicineName(),
                recall.getBatchNumber(),
                recall.getRecallReason(),
                recall.isActive(),
                recall.getRecalledAt()
        );
    }

    private int parsePrescribedQuantity(PrescriptionItem item) {
        try {
            String dur = item.getDuration() != null ? item.getDuration().replaceAll("[^0-9]", "").trim() : "1";
            int durationDays = Math.max(1, Integer.parseInt(dur));
            String freq = item.getFrequency() != null ? item.getFrequency().toLowerCase() : "";
            int dosesPerDay = 1;
            if (freq.contains("3") || freq.contains("thrice") || freq.contains("tid")) dosesPerDay = 3;
            else if (freq.contains("2") || freq.contains("twice") || freq.contains("bid")) dosesPerDay = 2;
            else if (freq.contains("4") || freq.contains("qid")) dosesPerDay = 4;
            return durationDays * dosesPerDay;
        } catch (Exception e) {
            log.warn("Could not parse prescribed quantity for item {}, using default 30", item.getId());
            return 30;
        }
    }

    private MedicineStockResponse mapStock(MedicineStock s) {
        return new MedicineStockResponse(
                s.getId(), s.getMedicineName(), s.getBatchNumber(),
                s.getExpiryDate(), s.getAvailableQuantity(), s.getQuantityReserved(),
                s.getEffectiveQuantity(), s.getReorderLevel(), s.getUnitPrice(),
                s.getPurchasePrice(), s.getSellingPrice(),
                s.getSupplier() != null ? s.getSupplier().getId() : null,
                s.getSupplier() != null ? s.getSupplier().getName() : null,
                s.isExpired(), s.isLowStock());
    }

    private DispensationResponse mapDispensation(DispensationRecord r, List<DispenseBatchResponse> batches) {
        return new DispensationResponse(
                r.getId(), r.getMedicineName(), r.getQuantityDispensed(),
                r.getRemainingQuantity(),
                r.getDispensationStatus() != null ? r.getDispensationStatus().name() : "COMPLETE",
                r.getTotalPrice(),
                r.getBillingStatus().name(),
                r.getDispensedAt(),
                r.getDispensedBy().getFullName(),
                batches);
    }
}
