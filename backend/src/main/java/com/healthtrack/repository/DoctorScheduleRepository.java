package com.healthtrack.repository;

import com.healthtrack.entity.DoctorSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Long> {

    List<DoctorSchedule> findByDoctorIdOrderByDayOfWeekAscStartTimeAsc(Long doctorId);

    List<DoctorSchedule> findByHospitalIdOrderByDoctorIdAscDayOfWeekAsc(Long hospitalId);
}
