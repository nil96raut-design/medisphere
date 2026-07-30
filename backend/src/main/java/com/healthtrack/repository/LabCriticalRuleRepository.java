package com.healthtrack.repository;

import com.healthtrack.entity.LabCriticalRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LabCriticalRuleRepository extends JpaRepository<LabCriticalRule, Long> {

    List<LabCriticalRule> findByHospitalIdAndTestNameAndEnabledTrue(Long hospitalId, String testName);

    List<LabCriticalRule> findByHospitalId(Long hospitalId);

    Optional<LabCriticalRule> findByIdAndHospitalId(Long id, Long hospitalId);
}
