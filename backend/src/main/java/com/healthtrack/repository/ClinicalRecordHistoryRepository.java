package com.healthtrack.repository;

import com.healthtrack.entity.ClinicalRecordHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClinicalRecordHistoryRepository extends JpaRepository<ClinicalRecordHistory, Long> {

    List<ClinicalRecordHistory> findByRecordTypeAndRecordIdOrderByVersionNumberDesc(String recordType, Long recordId);

    Optional<ClinicalRecordHistory> findTopByRecordTypeAndRecordIdOrderByVersionNumberDesc(String recordType, Long recordId);
}
