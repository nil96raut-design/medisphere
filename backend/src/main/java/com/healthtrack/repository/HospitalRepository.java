package com.healthtrack.repository;

import com.healthtrack.entity.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalRepository extends JpaRepository<Hospital, Long> {
    boolean existsByLicenseNumber(String licenseNumber);
    java.util.Optional<Hospital> findByInvitationCode(String invitationCode);
}
