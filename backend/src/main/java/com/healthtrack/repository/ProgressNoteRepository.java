package com.healthtrack.repository;

import com.healthtrack.entity.ProgressNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProgressNoteRepository extends JpaRepository<ProgressNote, Long> {
    List<ProgressNote> findByTaskIdOrderByCreatedAtDesc(Long taskId);
    List<ProgressNote> findByTaskAssigneeIdOrderByCreatedAtDesc(Long assigneeId);
}
