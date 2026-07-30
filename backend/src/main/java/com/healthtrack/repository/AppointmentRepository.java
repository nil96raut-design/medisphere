package com.healthtrack.repository;

import com.healthtrack.entity.Appointment;
import com.healthtrack.entity.AppointmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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

    Page<Appointment> findByPatientId(Long patientId, Pageable pageable);

    @Query("SELECT a.status, COUNT(a) FROM Appointment a WHERE a.hospital.id = :hospitalId GROUP BY a.status")
    List<Object[]> countByHospitalIdGroupByStatus(@Param("hospitalId") Long hospitalId);

    long countByHospitalIdAndAppointmentDate(Long hospitalId, LocalDate appointmentDate);

    @Query("SELECT a FROM Appointment a JOIN FETCH a.doctor d JOIN FETCH d.user WHERE a.patient.id = :patientId")
    List<Appointment> findByPatientIdWithDoctor(@Param("patientId") Long patientId);

    @EntityGraph(attributePaths = {"doctor", "doctor.user"})
    List<Appointment> findByPatientIdAndAppointmentDateGreaterThanEqualOrderByAppointmentDateAscStartTimeAsc(
            Long patientId, LocalDate appointmentDate);

    @Query(value = "SELECT EXTRACT(HOUR FROM a.start_time) as hour, COUNT(*) FROM appointment a " +
           "WHERE a.hospital_id = :hospitalId AND a.appointment_date = :date " +
           "GROUP BY EXTRACT(HOUR FROM a.start_time) ORDER BY hour", nativeQuery = true)
    List<Object[]> countByHospitalIdAndDateGroupByHour(@Param("hospitalId") Long hospitalId, @Param("date") LocalDate date);

    @Query("select a from Appointment a where a.appointmentDate < :date and a.status in ('SCHEDULED', 'CHECKED_IN')")
    List<Appointment> findMissedAppointments(@Param("date") LocalDate date);

    @Query("select a from Appointment a where a.hospital.id = :hospitalId and a.appointmentDate = :date order by a.startTime")
    List<Appointment> findByHospitalIdAndAppointmentDateOrderByStartTimeAsc(
            @Param("hospitalId") Long hospitalId, @Param("date") LocalDate date);
}