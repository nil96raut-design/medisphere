package com.healthtrack.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmergencyAlertService {

    private static final Logger log = LoggerFactory.getLogger(EmergencyAlertService.class);

    private final SseNotificationService sseNotificationService;

    public void triggerEmergencyAlert(Long hospitalId, Long patientId, String alertType, String message) {
        log.error("CRITICAL EMERGENCY ALERT for Hospital ID {}, Patient ID {}: [{}] {}",
                hospitalId, patientId, alertType, message);

        Map<String, Object> alertPayload = new HashMap<>();
        alertPayload.put("hospitalId", hospitalId);
        alertPayload.put("patientId", patientId);
        alertPayload.put("alertType", alertType);
        alertPayload.put("message", message);
        alertPayload.put("timestamp", OffsetDateTime.now().toString());
        alertPayload.put("priority", "CRITICAL");

        // Broadcast real-time SSE emergency alert to all active medical staff
        sseNotificationService.broadcastToHospital(hospitalId, "EMERGENCY_ALERT", alertPayload);
    }
}
