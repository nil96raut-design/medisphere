package com.healthtrack.repository;

import com.healthtrack.entity.WriteBufferEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WriteBufferRepository extends JpaRepository<WriteBufferEntry, Long> {
    
    List<WriteBufferEntry> findTop50ByStatusOrderByPriorityAscCreatedAtAsc(String status);
    
    List<WriteBufferEntry> findByStatusOrderByCreatedAtDesc(String status);
    
    long countByStatus(String status);
}
