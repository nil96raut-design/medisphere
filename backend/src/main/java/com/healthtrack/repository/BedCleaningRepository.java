package com.healthtrack.repository;

import com.healthtrack.entity.BedCleaningRequest;
import com.healthtrack.entity.CleaningStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BedCleaningRepository extends JpaRepository<BedCleaningRequest, Long> {

    List<BedCleaningRequest> findByStatusOrderByCreatedAtDesc(CleaningStatus status);

    List<BedCleaningRequest> findByBedIdOrderByCreatedAtDesc(Long bedId);

    Optional<BedCleaningRequest> findTopByBedIdAndStatusOrderByCreatedAtDesc(Long bedId, CleaningStatus status);

    boolean existsByBedIdAndStatus(Long bedId, CleaningStatus status);
}
