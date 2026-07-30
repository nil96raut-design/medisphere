package com.healthtrack.service;

import com.healthtrack.entity.BackgroundJob;
import com.healthtrack.repository.BackgroundJobRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JobMonitoringService {

    private static final Logger log = LoggerFactory.getLogger(JobMonitoringService.class);

    private final BackgroundJobRepository backgroundJobRepository;

    @Transactional
    public void recordJobStart(String jobType, String payload) {
        BackgroundJob job = BackgroundJob.builder()
                .jobType(jobType)
                .payload(payload)
                .status("RUNNING")
                .createdAt(OffsetDateTime.now())
                .build();
        backgroundJobRepository.save(job);
        log.info("Background job {} started", jobType);
    }

    @Transactional
    public void recordJobSuccess(String jobType) {
        List<BackgroundJob> runningJobs = backgroundJobRepository.findByStatus("RUNNING");
        for (BackgroundJob job : runningJobs) {
            if (jobType.equals(job.getJobType())) {
                job.setStatus("SUCCESS");
                job.setCompletedAt(OffsetDateTime.now());
                backgroundJobRepository.save(job);
            }
        }
    }

    @Transactional
    public void recordJobFailure(String jobType, String errorMessage) {
        List<BackgroundJob> runningJobs = backgroundJobRepository.findByStatus("RUNNING");
        for (BackgroundJob job : runningJobs) {
            if (jobType.equals(job.getJobType())) {
                job.setStatus("FAILED");
                job.setLastError(errorMessage);
                job.setCompletedAt(OffsetDateTime.now());
                backgroundJobRepository.save(job);
            }
        }
        log.error("Background job {} failed: {}", jobType, errorMessage);
    }

    @Transactional(readOnly = true)
    public List<BackgroundJob> getRecentJobs() {
        return backgroundJobRepository.findAll();
    }
}
