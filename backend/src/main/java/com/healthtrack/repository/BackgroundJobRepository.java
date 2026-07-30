package com.healthtrack.repository;

import com.healthtrack.entity.BackgroundJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface BackgroundJobRepository extends JpaRepository<BackgroundJob, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT j FROM BackgroundJob j WHERE j.status = 'PENDING' AND (j.nextAttemptAt IS NULL OR j.nextAttemptAt <= :now) AND (j.lockedUntil IS NULL OR j.lockedUntil < :now) ORDER BY j.priority, j.createdAt")
    List<BackgroundJob> findNextPendingBatch(@Param("now") OffsetDateTime now);

    List<BackgroundJob> findByStatus(String status);

    List<BackgroundJob> findByStatusOrderByCreatedAtAsc(String status);

    long countByStatus(String status);
}
