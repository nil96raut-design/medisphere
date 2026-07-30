package com.healthtrack.repository;

import com.healthtrack.entity.ShiftHandover;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShiftHandoverRepository extends JpaRepository<ShiftHandover, Long> {

    List<ShiftHandover> findByFromNurseIdOrderByCreatedAtDesc(Long fromNurseId);

    List<ShiftHandover> findByToNurseIdOrderByCreatedAtDesc(Long toNurseId);
}
