package com.healthtrack.repository;

import com.healthtrack.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, Long> {

    Optional<IdempotencyRecord> findByRequestId(String requestId);

    boolean existsByRequestId(String requestId);
}
