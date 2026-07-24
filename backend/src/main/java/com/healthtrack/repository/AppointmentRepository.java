package com.healthtrack.repository;

import com.healthtrack.entity.Appointment;
import com.healthtrack.entity.AppointmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select a from Appointment a
        where a.doctor.id = :doctorId
          and a.appointmentDate = :date
          and a.startTime < :endTime
          and a.endTime > :startTime
    """)
    List<Appointment> findOverlappingLocked(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Appointment a where a.doctor.id = :doctorId and a.appointmentDate = :date and a.status <> 'CANCELLED'")
    List<Appointment> findLastTokenLocked(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date
    );

    List<Appointment> findByDoctorIdAndAppointmentDateAndStatusNotOrderByTokenNumberAsc(
            Long doctorId, LocalDate appointmentDate, AppointmentStatus status);

    long countByAppointmentDate(LocalDate appointmentDate);

    List<Appointment> findByPatientId(Long patientId);
}
