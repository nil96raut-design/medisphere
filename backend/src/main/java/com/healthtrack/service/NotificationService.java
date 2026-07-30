package com.healthtrack.service;

import com.healthtrack.entity.Hospital;
import com.healthtrack.entity.Notification;
import com.healthtrack.entity.Role;
import com.healthtrack.entity.User;
import com.healthtrack.event.HmsEvent;
import com.healthtrack.repository.HospitalRepository;
import com.healthtrack.repository.NotificationRepository;
import com.healthtrack.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final HospitalRepository hospitalRepository;
    private final EntityManager entityManager;
    private final BackgroundJobRunner backgroundJobRunner;

    @Autowired(required = false)
    @Lazy
    private EmailService emailService;

    private static final String CHANNEL_IN_APP = "IN_APP";
    private static final String CHANNEL_EMAIL = "EMAIL";

    @Transactional
    public Notification sendInApp(Long userId, Long hospitalId, String type, String title, String message,
                                   String refType, Long refId) {
        User user = userRepository.findById(userId).orElse(null);
        Hospital hospital = hospitalRepository.findById(hospitalId).orElse(null);
        if (user == null || hospital == null) return null;

        Notification notif = Notification.builder()
                .user(user)
                .hospital(hospital)
                .type(type)
                .title(title)
                .message(message)
                .status("SENT")
                .channel(CHANNEL_IN_APP)
                .referenceType(refType)
                .referenceId(refId)
                .sentAt(java.time.OffsetDateTime.now())
                .build();
        return notificationRepository.save(notif);
    }

    @Transactional
    public void sendEmail(Long userId, Long hospitalId, String type, String title, String message,
                           String refType, Long refId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getEmail() == null) return;

        Notification notif = Notification.builder()
                .user(user)
                .hospital(hospitalRepository.getReferenceById(hospitalId))
                .type(type)
                .title(title)
                .message(message)
                .status("PENDING")
                .channel(CHANNEL_EMAIL)
                .referenceType(refType)
                .referenceId(refId)
                .sentAt(java.time.OffsetDateTime.now())
                .build();
        notif = notificationRepository.save(notif);

        backgroundJobRunner.enqueue("SEND_EMAIL", """
                {"notificationId":%d,"email":"%s","subject":"%s","body":"%s"}
                """.formatted(notif.getId(), user.getEmail(), title,
                message.replace("\"", "'")), 5);
    }

    @Async
    @EventListener(condition = "#event.eventType == 'APPOINTMENT_CREATED'")
    public void onAppointmentCreated(HmsEvent event) {
        try {
            @SuppressWarnings("unchecked")
            var data = (java.util.Map<String, Object>) event.getPayload();
            Long patientId = ((Number) data.get("patientId")).longValue();
            Long hospitalId = event.getHospitalId();
            User patient = findPatientUser(patientId, hospitalId);
            if (patient != null) {
                sendInApp(patient.getId(), hospitalId, "APPOINTMENT_BOOKED",
                        "Appointment Confirmed",
                        "Your appointment on " + data.get("date") + " at " + data.get("time") + " is confirmed.",
                        "APPOINTMENT", ((Number) data.get("appointmentId")).longValue());

                if (patient.getEmail() != null) {
                    String html = emailService.buildAppointmentConfirmation(
                            patient.getFullName(),
                            (String) data.getOrDefault("doctorName", "Doctor"),
                            String.valueOf(data.get("date")),
                            String.valueOf(data.get("time")));
                    sendEmail(patient.getId(), hospitalId, "APPOINTMENT_BOOKED",
                            "Appointment Confirmed",
                            html, "APPOINTMENT", ((Number) data.get("appointmentId")).longValue());
                }
            }
        } catch (Exception e) {
            log.error("Failed to send appointment notification", e);
        }
    }

    @Async
    @EventListener(condition = "#event.eventType == 'LAB_RESULT_READY'")
    public void onLabResultReady(HmsEvent event) {
        try {
            @SuppressWarnings("unchecked")
            var data = (java.util.Map<String, Object>) event.getPayload();
            Long patientId = ((Number) data.get("patientId")).longValue();
            Long hospitalId = event.getHospitalId();
            User patient = findPatientUser(patientId, hospitalId);
            if (patient != null) {
                String testName = (String) data.getOrDefault("testName", "Lab Test");
                sendInApp(patient.getId(), hospitalId, "LAB_RESULT",
                        "Lab Results Ready",
                        "Your " + testName + " results are ready for review.",
                        "LAB_ORDER", ((Number) data.get("orderId")).longValue());

                if (patient.getEmail() != null) {
                    String html = emailService.buildLabResultNotification(patient.getFullName(), testName);
                    sendEmail(patient.getId(), hospitalId, "LAB_RESULT",
                            "Lab Results Ready",
                            html, "LAB_ORDER", ((Number) data.get("orderId")).longValue());
                }
            }
        } catch (Exception e) {
            log.error("Failed to send lab result notification", e);
        }
    }

    @Async
    @EventListener(condition = "#event.eventType == 'BILL_GENERATED'")
    public void onBillGenerated(HmsEvent event) {
        try {
            @SuppressWarnings("unchecked")
            var data = (java.util.Map<String, Object>) event.getPayload();
            Long patientId = ((Number) data.get("patientId")).longValue();
            Long hospitalId = event.getHospitalId();
            User patient = findPatientUser(patientId, hospitalId);
            if (patient != null) {
                String amount = String.valueOf(data.get("amount"));
                sendInApp(patient.getId(), hospitalId, "BILL_GENERATED",
                        "Bill Generated",
                        "Your bill of $" + amount + " has been generated.",
                        "BILL", ((Number) data.get("billId")).longValue());

                if (patient.getEmail() != null) {
                    String html = emailService.buildBillNotification(patient.getFullName(), amount,
                            String.valueOf(data.get("billId")));
                    sendEmail(patient.getId(), hospitalId, "BILL_GENERATED",
                            "Bill Generated",
                            html, "BILL", ((Number) data.get("billId")).longValue());
                }
            }
        } catch (Exception e) {
            log.error("Failed to send bill notification", e);
        }
    }

    @Async
    @EventListener(condition = "#event.eventType == 'PRESCRIPTION_DISPENSED'")
    public void onPrescriptionDispensed(HmsEvent event) {
        try {
            @SuppressWarnings("unchecked")
            var data = (java.util.Map<String, Object>) event.getPayload();
            Long patientId = ((Number) data.get("patientId")).longValue();
            Long hospitalId = event.getHospitalId();
            User patient = findPatientUser(patientId, hospitalId);
            if (patient != null) {
                sendInApp(patient.getId(), hospitalId, "MEDICATION_DISPENSED",
                        "Medication Dispensed",
                        data.get("medicineName") + " x" + data.get("quantity") + " has been dispensed.",
                        "DISPENSATION", ((Number) data.get("dispensationId")).longValue());
            }
        } catch (Exception e) {
            log.error("Failed to send dispensation notification", e);
        }
    }

    @Async
    @EventListener(condition = "#event.eventType == 'ALERT_CREATED'")
    public void onAlertCreated(HmsEvent event) {
        try {
            @SuppressWarnings("unchecked")
            var data = (java.util.Map<String, Object>) event.getPayload();
            Long patientId = ((Number) data.get("patientId")).longValue();
            Long hospitalId = event.getHospitalId();
            String severity = (String) data.get("severity");
            String message = (String) data.get("message");

            List<User> nurses = userRepository.findByHospitalId(hospitalId).stream()
                    .filter(u -> u.getRole() == Role.NURSE).toList();
            for (User nurse : nurses) {
                sendInApp(nurse.getId(), hospitalId, "ALERT_" + severity,
                        "[" + severity + "] Clinical Alert",
                        message, "ALERT", ((Number) data.get("alertId")).longValue());
            }
        } catch (Exception e) {
            log.error("Failed to send alert notification", e);
        }
    }

    @Async
    @EventListener(condition = "#event.eventType == 'MEDICATION_MISSED'")
    public void onMedicationMissed(HmsEvent event) {
        try {
            @SuppressWarnings("unchecked")
            var data = (java.util.Map<String, Object>) event.getPayload();
            Long hospitalId = event.getHospitalId();
            List<User> nurses = userRepository.findByHospitalId(hospitalId).stream()
                    .filter(u -> u.getRole() == Role.NURSE).toList();
            for (User nurse : nurses) {
                sendInApp(nurse.getId(), hospitalId, "MEDICATION_MISSED",
                        "Medication Dose Missed",
                        "Patient " + data.get("patientId") + " missed scheduled medication dose.",
                        "MEDICATION_SCHEDULE", ((Number) data.get("scheduleId")).longValue());
            }
        } catch (Exception e) {
            log.error("Failed to send medication missed notification", e);
        }
    }

    @Async
    @EventListener(condition = "#event.eventType == 'CRITICAL_VITAL_ESCALATED'")
    public void onCriticalVitalEscalated(HmsEvent event) {
        try {
            @SuppressWarnings("unchecked")
            var data = (java.util.Map<String, Object>) event.getPayload();
            Long patientId = ((Number) data.get("patientId")).longValue();
            Long hospitalId = event.getHospitalId();

            List<User> doctors = userRepository.findByHospitalId(hospitalId).stream()
                    .filter(u -> u.getRole() == Role.DOCTOR).toList();
            for (User doctor : doctors) {
                sendInApp(doctor.getId(), hospitalId, "CRITICAL_VITAL_ESCALATED",
                        "Critical Vital Signs - Immediate Attention",
                        "Patient " + patientId + " has " + data.get("consecutiveAbnormalCount") + " consecutive abnormal vitals.",
                        "PATIENT", patientId);
            }
        } catch (Exception e) {
            log.error("Failed to send critical vital escalation notification", e);
        }
    }

    @Async
    @EventListener(condition = "#event.eventType == 'CRITICAL_LAB_RESULT'")
    public void onCriticalLabResult(HmsEvent event) {
        try {
            @SuppressWarnings("unchecked")
            var data = (java.util.Map<String, Object>) event.getPayload();
            Long patientId = ((Number) data.get("patientId")).longValue();
            Long hospitalId = event.getHospitalId();
            String testName = (String) data.getOrDefault("testName", "Lab Test");

            List<User> doctors = userRepository.findByHospitalId(hospitalId).stream()
                    .filter(u -> u.getRole() == Role.DOCTOR).toList();
            for (User doctor : doctors) {
                sendInApp(doctor.getId(), hospitalId, "CRITICAL_LAB_RESULT",
                        "Critical Lab Result - Immediate Attention",
                        "Patient " + patientId + " has a critical result for " + testName + ".",
                        "LAB_ORDER", ((Number) data.get("orderId")).longValue());
            }
        } catch (Exception e) {
            log.error("Failed to send critical lab result notification", e);
        }
    }

    private User findPatientUser(Long patientId, Long hospitalId) {
        @SuppressWarnings("unchecked")
        List<User> users = entityManager.createQuery(
                "SELECT u FROM User u WHERE u.role = com.healthtrack.entity.Role.PATIENT " +
                "AND u.id = :pid AND u.hospital.id = :hid")
                .setParameter("pid", patientId)
                .setParameter("hid", hospitalId)
                .getResultList();
        return users.isEmpty() ? null : users.get(0);
    }
}
