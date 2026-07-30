package com.healthtrack.repository;

import com.healthtrack.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByPhoneNumber(String phoneNumber);

    Optional<Patient> findByEmail(String email);

    Page<Patient> findByHospitalId(Long hospitalId, Pageable pageable);

    @Query("SELECT p FROM Patient p WHERE p.hospital.id = :hospitalId AND (LOWER(p.firstName) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :q, '%')) OR p.phoneNumber LIKE CONCAT('%', :q, '%'))")
    Page<Patient> searchPatients(@Param("q") String q, @Param("hospitalId") Long hospitalId, Pageable pageable);

    @Query("select count(p) from Patient p where p.hospital.id = :hospitalId and p.createdAt between :start and :end")
    long countByHospitalIdAndCreatedAtBetween(
            @Param("hospitalId") Long hospitalId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}