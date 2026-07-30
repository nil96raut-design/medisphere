package com.healthtrack.repository;

import com.healthtrack.entity.Doctor;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    @Query("SELECT d FROM Doctor d WHERE d.isAvailable = true")
    List<Doctor> findAvailableDoctors();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Doctor d WHERE d.id = :id")
    Optional<Doctor> findByIdLocked(@Param("id") Long id);

    Optional<Doctor> findByUserId(Long userId);

    List<Doctor> findByHospitalId(Long hospitalId);

    @Query(value = """
        select u.full_name as name, count(a.id) as cnt
        from doctor d
        join app_user u on u.id = d.user_id
        join appointment a on a.doctor_id = d.id
        where d.hospital_id = :hospitalId
        group by d.id, u.full_name
        order by cnt desc
    """, nativeQuery = true)
    List<Object[]> findTopByHospitalIdOrderByAppointmentCountDesc(
            @Param("hospitalId") Long hospitalId,
            org.springframework.data.domain.Pageable pageable);
}
