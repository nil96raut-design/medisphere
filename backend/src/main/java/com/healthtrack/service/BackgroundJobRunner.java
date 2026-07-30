package com.healthtrack.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthtrack.entity.Appointment;
import com.healthtrack.entity.AppointmentStatus;
import com.healthtrack.entity.BackgroundJob;
import com.healthtrack.entity.Notification;
import com.healthtrack.repository.AppointmentRepository;
import com.healthtrack.repository.BackgroundJobRepository;
import com.healthtrack.repository.NotificationRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.mail.MailException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class BackgroundJobRunner {

    private static final Logger log = LoggerFactory.getLogger(BackgroundJobRunner.class);

    private final BackgroundJobRepository backgroundJobRepository;
    private final NotificationRepository notificationRepository;
    private final AppointmentRepository appointmentRepository;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    @Lazy
    private EmailService emailService;

    private final Map<String, JobHandler> handlers = new ConcurrentHashMap<>();

    @PostConstruct
    public void registerHandlers() {
        handlers.put("SEND_EMAIL", payload -> {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = objectMapper.readValue(payload, Map.class);
                String email = (String) data.get("email");
                String subject = (String) data.get("subject");
                String body = (String) data.get("body");
                Long notificationId = ((Number) data.get("notificationId")).longValue();

                if (emailService != null) {
                    emailService.sendEmail(email, subject, body);
                }

                notificationRepository.findById(notificationId).ifPresent(n -> {
                    n.setStatus("SENT");
                    n.setSentAt(OffsetDateTime.now());
                    notificationRepository.save(n);
                });
                return true;
            } catch (MailException e) {
                log.warn("Email send failed (will retry): {}", e.getMessage());
                return false;
            } catch (Exception e) {
                log.error("Email job failed permanently: {}", e.getMessage());
                return false;
            }
        });

        handlers.put("SEND_NOTIFICATION", payload -> true);
    }

    @Scheduled(fixedRate = 10_000)
    @Transactional
    public void processPendingJobs() {
        List<BackgroundJob> jobs = backgroundJobRepository.findNextPendingBatch(OffsetDateTime.now());
        for (BackgroundJob job : jobs) {
            processJobAsync(job);
        }
    }

    @Async
    @Transactional
    public void processJobAsync(BackgroundJob job) {
        try {
            job.setLockedUntil(OffsetDateTime.now().plusMinutes(5));
            backgroundJobRepository.save(job);

            JobHandler handler = handlers.get(job.getJobType());
            boolean success = handler != null && handler.handle(job.getPayload());

            if (success) {
                job.setStatus("COMPLETED");
                job.setCompletedAt(OffsetDateTime.now());
            } else {
                throw new RuntimeException("Handler returned false");
            }
        } catch (Exception e) {
            job.setRetryCount(job.getRetryCount() + 1);
            job.setLastError(e.getMessage() != null ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 500)) : "Unknown error");
            if (job.getRetryCount() >= job.getMaxRetries()) {
                job.setStatus("FAILED");
                log.error("Job {} permanently failed after {} retries: {}", job.getId(), job.getMaxRetries(), e.getMessage());
            } else {
                long delaySeconds = (long) Math.pow(2, job.getRetryCount()) * 10;
                job.setNextAttemptAt(OffsetDateTime.now().plusSeconds(delaySeconds));
                log.warn("Job {} failed (attempt {}/{}), retrying in {}s: {}",
                        job.getId(), job.getRetryCount(), job.getMaxRetries(), delaySeconds, e.getMessage());
            }
        } finally {
            job.setLockedUntil(null);
            backgroundJobRepository.save(job);
        }
    }

    @Transactional
    public BackgroundJob enqueue(String jobType, String payload, int priority) {
        BackgroundJob job = BackgroundJob.builder()
                .jobType(jobType)
                .payload(payload)
                .priority(priority)
                .status("PENDING")
                .nextAttemptAt(OffsetDateTime.now())
                .build();
        return backgroundJobRepository.save(job);
    }

    @Transactional
    public void enqueueBatch(String jobType, List<String> payloads, int priority) {
        List<BackgroundJob> jobs = payloads.stream()
                .map(p -> BackgroundJob.builder()
                        .jobType(jobType).payload(p).priority(priority)
                        .status("PENDING").nextAttemptAt(OffsetDateTime.now()).build())
                .toList();
        backgroundJobRepository.saveAll(jobs);
    }

    @Scheduled(cron = "0 0/30 * * * *")
    @Transactional
    public void autoMarkNoShow() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<Appointment> missed = appointmentRepository.findMissedAppointments(yesterday);
        for (Appointment a : missed) {
            a.setStatus(AppointmentStatus.NO_SHOW);
        }
        if (!missed.isEmpty()) {
            appointmentRepository.saveAll(missed);
            log.info("Auto-marked {} appointment(s) as NO_SHOW", missed.size());
        }
    }

    public interface JobHandler {
        boolean handle(String payload);
    }
}
