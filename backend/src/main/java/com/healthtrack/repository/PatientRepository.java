package com.healthtrack.repository;

import com.healthtrack.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByPhoneNumber(String phoneNumber);
    
    @Query("SELECT p FROM Patient p WHERE lower(p.firstName) LIKE lower(concat('%', :q, '%')) OR lower(p.lastName) LIKE lower(concat('%', :q, '%')) OR p.phoneNumber LIKE concat('%', :q, '%')")
    List<Patient> searchPatients(@Param("q") String q);
}
