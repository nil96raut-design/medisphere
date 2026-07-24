package com.healthtrack.repository;

import com.healthtrack.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByAssigneeId(Long assigneeId);
    List<Task> findByAssignedById(Long assignedById);
    List<Task> findAllByOrderByCreatedAtDesc();

    // q matches title or description, case-insensitive; pass null/blank q to skip the filter.
    @Query("""
            select t from Task t
            where t.assignee.id = :assigneeId
              and (:q is null or :q = '' or
                   lower(t.title) like lower(concat('%', :q, '%')) or
                   lower(t.description) like lower(concat('%', :q, '%')))
            """)
    Page<Task> searchByAssignee(@Param("assigneeId") Long assigneeId, @Param("q") String q, Pageable pageable);

    @Query("""
            select t from Task t
            where (:q is null or :q = '' or
                   lower(t.title) like lower(concat('%', :q, '%')) or
                   lower(t.description) like lower(concat('%', :q, '%')))
            """)
    Page<Task> searchAll(@Param("q") String q, Pageable pageable);
}
