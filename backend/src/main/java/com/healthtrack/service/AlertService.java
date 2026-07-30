package com.healthtrack.service;

import com.healthtrack.dto.ClinicalSafetyDtos.AlertResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final AlertRepository alertRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final TenantValidator tenantValidator;
    private final EventPublisher eventPublisher;
    private final NotificationService notificationService;

    @Transactional
    public AlertResponse createAlert(Long hospitalId, Long patientId, AlertType type,
                                     AlertSeverity severity, String message) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));

        Alert alert = Alert.builder()
                .hospital(patient.getHospital())
                .patient(patient)
                .type(type)
                .severity(severity)
                .message(message)
                .build();
        alert = alertRepository.save(alert);

        eventPublisher.publish(EventConstants.ALERT_CREATED, hospitalId,
                Map.of("alertId", alert.getId(), "patientId", patientId,
                        "type", type.name(), "severity", severity.name(),
                        "message", message, "hospitalId", hospitalId));

        log.warn("ALERT created: [{}] {} - {}", severity, type, message);
        return mapAlertResponse(alert);
    }

    @Transactional
    @CacheEvict(value = "activeAlerts", key = "#currentUser.getHospitalId()")
    public AlertResponse acknowledgeAlert(Long alertId, UserPrincipal currentUser) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found"));
        tenantValidator.validateHospitalAccess(alert.getHospital().getId(), currentUser.getHospitalId());

        if (alert.getStatus() == AlertStatus.RESOLVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Alert already resolved");
        }

        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedBy(currentUser.getUser());
        alert.setAcknowledgedAt(OffsetDateTime.now());
        alert = alertRepository.save(alert);

        eventPublisher.publish(EventConstants.ALERT_ACKNOWLEDGED, currentUser.getHospitalId(),
                Map.of("alertId", alert.getId(), "acknowledgedBy", currentUser.getUser().getId(),
                        "hospitalId", currentUser.getHospitalId()));

        return mapAlertResponse(alert);
    }

    @Transactional
    @CacheEvict(value = "activeAlerts", key = "#currentUser.getHospitalId()")
    public AlertResponse resolveAlert(Long alertId, UserPrincipal currentUser) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found"));
        tenantValidator.validateHospitalAccess(alert.getHospital().getId(), currentUser.getHospitalId());

        alert.setStatus(AlertStatus.RESOLVED);
        alert.setResolvedBy(currentUser.getUser());
        alert.setResolvedAt(OffsetDateTime.now());
        alert = alertRepository.save(alert);

        eventPublisher.publish(EventConstants.ALERT_RESOLVED, currentUser.getHospitalId(),
                Map.of("alertId", alert.getId(), "resolvedBy", currentUser.getUser().getId(),
                        "hospitalId", currentUser.getHospitalId()));

        return mapAlertResponse(alert);
    }

    @Scheduled(fixedRate = 120000)
    @Transactional
    public void escalateAlerts() {
        OffsetDateTime now = OffsetDateTime.now();

        List<Alert> activeAlerts = alertRepository.findByStatusAndCreatedAtBefore(
                AlertStatus.ACTIVE, now.minusMinutes(5));
        for (Alert alert : activeAlerts) {
            User doctor = userRepository.findFirstByHospitalIdAndRole(
                    alert.getHospital().getId(), Role.DOCTOR);
            if (doctor != null) {
                alert.setStatus(AlertStatus.ESCALATED);
                alert.setEscalatedTo(doctor);
                alert.setEscalatedAt(now);
                alertRepository.save(alert);

                notificationService.sendInApp(doctor.getId(), alert.getHospital().getId(),
                        "ALERT_ESCALATED", "Critical Alert Requires Attention",
                        "Alert: " + alert.getMessage(), "ALERT", alert.getId());

                eventPublisher.publish(EventConstants.ALERT_ESCALATED, alert.getHospital().getId(),
                        Map.of("alertId", alert.getId(), "escalatedTo", doctor.getId(),
                                "patientId", alert.getPatient().getId(),
                                "hospitalId", alert.getHospital().getId()));
                log.warn("Alert {} escalated to doctor {}", alert.getId(), doctor.getId());
            }
        }

        List<Alert> escalatedAlerts = alertRepository.findByStatusAndCreatedAtBefore(
                AlertStatus.ESCALATED, now.minusMinutes(10));
        for (Alert alert : escalatedAlerts) {
            User admin = userRepository.findFirstByHospitalIdAndRole(
                    alert.getHospital().getId(), Role.ADMIN);
            if (admin != null) {
                alert.setEscalatedTo(admin);
                alert.setEscalatedAt(now);
                alertRepository.save(alert);

                notificationService.sendInApp(admin.getId(), alert.getHospital().getId(),
                        "ALERT_ESCALATED_ADMIN", "Escalated Alert - Admin Required",
                        "Alert: " + alert.getMessage() + " - requires admin intervention",
                        "ALERT", alert.getId());

                eventPublisher.publish(EventConstants.ALERT_ESCALATED, alert.getHospital().getId(),
                        Map.of("alertId", alert.getId(), "escalatedTo", admin.getId(),
                                "patientId", alert.getPatient().getId(),
                                "hospitalId", alert.getHospital().getId()));
                log.warn("Alert {} escalated to admin {}", alert.getId(), admin.getId());
            }
        }
    }

    @Cacheable(value = "activeAlerts", key = "#hospitalId")
    @Transactional(readOnly = true)
    public List<AlertResponse> getActiveAlerts(Long hospitalId) {
        return alertRepository.findByStatusFetching(AlertStatus.ACTIVE).stream()
                .filter(a -> a.getHospital().getId().equals(hospitalId))
                .map(this::mapAlertResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> getPatientAlerts(Long patientId, UserPrincipal currentUser) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
        tenantValidator.validateHospitalAccess(patient.getHospital().getId(), currentUser.getHospitalId());
        return alertRepository.findByPatientIdOrderByCreatedAtDesc(patientId).stream()
                .map(this::mapAlertResponse).toList();
    }

    private AlertResponse mapAlertResponse(Alert a) {
        return new AlertResponse(
                a.getId(), a.getPatient().getId(),
                a.getPatient().getFirstName() + " " + a.getPatient().getLastName(),
                a.getType(), a.getSeverity(), a.getStatus(), a.getMessage(),
                a.getCreatedAt(),
                a.getAcknowledgedBy() != null ? a.getAcknowledgedBy().getId() : null,
                a.getAcknowledgedBy() != null ? a.getAcknowledgedBy().getFullName() : null,
                a.getAcknowledgedAt(),
                a.getEscalatedTo() != null ? a.getEscalatedTo().getId() : null,
                a.getEscalatedTo() != null ? a.getEscalatedTo().getFullName() : null,
                a.getEscalatedAt(),
                a.getResolvedBy() != null ? a.getResolvedBy().getId() : null,
                a.getResolvedAt());
    }
}
