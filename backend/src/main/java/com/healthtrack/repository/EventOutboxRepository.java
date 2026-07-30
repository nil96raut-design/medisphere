package com.healthtrack.repository;

import com.healthtrack.entity.EventOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventOutboxRepository extends JpaRepository<EventOutbox, Long> {
    
    @Query("SELECT e FROM EventOutbox e WHERE e.status = 'PENDING' OR (e.status = 'FAILED' AND e.retryCount < 5) ORDER BY e.createdAt ASC")
    List<EventOutbox> findPendingEvents();
    
    List<EventOutbox> findByStatusOrderByCreatedAtDesc(String status);
}
