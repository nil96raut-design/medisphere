package com.healthtrack.repository;

import com.healthtrack.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByPhoneNumber(String phoneNumber);

    Page<Patient> findByHospitalId(Long hospitalId, Pageable pageable);
    
    @Query("SELECT p FROM Patient p WHERE p.hospital.id = :hospitalId AND (LOWER(p.firstName) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :q, '%')) OR p.phoneNumber LIKE CONCAT('%', :q, '%'))")
    Page<Patient> searchPatients(@Param("q") String q, @Param("hospitalId") Long hospitalId, Pageable pageable);
}
