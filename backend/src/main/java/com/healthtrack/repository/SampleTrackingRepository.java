package com.healthtrack.repository;

import com.healthtrack.entity.SampleTracking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SampleTrackingRepository extends JpaRepository<SampleTracking, Long> {

    Optional<SampleTracking> findByLabOrderId(Long labOrderId);

    List<SampleTracking> findByHospitalId(Long hospitalId);

    Optional<SampleTracking> findByBarcode(String barcode);
}
