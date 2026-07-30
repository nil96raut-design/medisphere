package com.healthtrack.repository;

import com.healthtrack.entity.SlaBreach;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SlaBreachRepository extends JpaRepository<SlaBreach, Long> {

    List<SlaBreach> findByHospitalIdAndNotifiedFalse(Long hospitalId);

    List<SlaBreach> findByHospitalIdOrderByBreachedAtDesc(Long hospitalId);

    boolean existsByLabOrderId(Long labOrderId);
}
