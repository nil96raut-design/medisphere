package com.healthtrack.service;

import com.healthtrack.dto.PharmacyDtos.ExpiryAlertResponse;
import com.healthtrack.entity.*;
import com.healthtrack.event.EventConstants;
import com.healthtrack.event.EventPublisher;
import com.healthtrack.repository.*;
import com.healthtrack.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExpiryAlertService {

    private static final Logger log = LoggerFactory.getLogger(ExpiryAlertService.class);

    private final ExpiryAlertRepository expiryAlertRepository;
    private final MedicineStockRepository medicineStockRepository;
    private final HospitalRepository hospitalRepository;
    private final EventPublisher eventPublisher;

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void checkExpiryAlerts() {
        log.info("Running daily expiry alert check");
        List<Hospital> hospitals = hospitalRepository.findAll();
        LocalDate today = LocalDate.now();
        LocalDate nearExpiryThreshold = today.plusDays(30);

        for (Hospital hospital : hospitals) {
            Long hospitalId = hospital.getId();
            List<MedicineStock> allStock = medicineStockRepository.findByHospitalId(hospitalId);

            for (MedicineStock stock : allStock) {
                if (stock.getExpiryDate() == null) continue;

                if (stock.getExpiryDate().isBefore(today)) {
                    if (!expiryAlertRepository.existsByMedicineStockIdAndAlertTypeAndIsResolvedFalse(
                            stock.getId(), ExpiryAlertType.EXPIRED)) {
                        ExpiryAlert alert = ExpiryAlert.builder()
                                .hospital(hospital)
                                .medicineStock(stock)
                                .medicineName(stock.getMedicineName())
                                .batchNumber(stock.getBatchNumber())
                                .expiryDate(stock.getExpiryDate())
                                .alertType(ExpiryAlertType.EXPIRED)
                                .build();
                        expiryAlertRepository.save(alert);

                        eventPublisher.publish(EventConstants.EXPIRY_ALERT, hospitalId,
                                Map.of("type", "EXPIRED", "medicineName", stock.getMedicineName(),
                                        "batchNumber", stock.getBatchNumber(), "expiryDate",
                                        stock.getExpiryDate().toString()));
                    }
                } else if (stock.getExpiryDate().isBefore(nearExpiryThreshold)) {
                    if (!expiryAlertRepository.existsByMedicineStockIdAndAlertTypeAndIsResolvedFalse(
                            stock.getId(), ExpiryAlertType.NEAR_EXPIRY)) {
                        ExpiryAlert alert = ExpiryAlert.builder()
                                .hospital(hospital)
                                .medicineStock(stock)
                                .medicineName(stock.getMedicineName())
                                .batchNumber(stock.getBatchNumber())
                                .expiryDate(stock.getExpiryDate())
                                .alertType(ExpiryAlertType.NEAR_EXPIRY)
                                .build();
                        expiryAlertRepository.save(alert);

                        eventPublisher.publish(EventConstants.NEAR_EXPIRY_ALERT, hospitalId,
                                Map.of("type", "NEAR_EXPIRY", "medicineName", stock.getMedicineName(),
                                        "batchNumber", stock.getBatchNumber(), "expiryDate",
                                        stock.getExpiryDate().toString(),
                                        "daysUntilExpiry",
                                        String.valueOf(LocalDate.now().until(stock.getExpiryDate()).getDays())));
                    }
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public List<ExpiryAlertResponse> getActiveAlerts(UserPrincipal currentUser) {
        return expiryAlertRepository.findByHospitalIdAndIsResolvedFalse(currentUser.getHospitalId())
                .stream().map(this::mapAlert).toList();
    }

    @Transactional(readOnly = true)
    public List<ExpiryAlertResponse> getAlertsByType(UserPrincipal currentUser, ExpiryAlertType type) {
        return expiryAlertRepository.findByHospitalIdAndAlertTypeAndIsResolvedFalse(
                currentUser.getHospitalId(), type).stream().map(this::mapAlert).toList();
    }

    @Transactional
    public void resolveAlert(Long alertId, UserPrincipal currentUser) {
        ExpiryAlert alert = expiryAlertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Expiry alert not found: " + alertId));
        alert.setIsResolved(true);
        alert.setResolvedAt(OffsetDateTime.now());
        expiryAlertRepository.save(alert);
    }

    private ExpiryAlertResponse mapAlert(ExpiryAlert a) {
        return new ExpiryAlertResponse(
                a.getId(), a.getMedicineStock().getId(),
                a.getMedicineName(), a.getBatchNumber(),
                a.getExpiryDate(), a.getAlertType().name(), a.getIsResolved());
    }
}
