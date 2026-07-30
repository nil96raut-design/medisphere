package com.healthtrack.repository;

import com.healthtrack.entity.TokenBlocklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TokenBlocklistRepository extends JpaRepository<TokenBlocklist, Long> {
    Optional<TokenBlocklist> findByToken(String token);
    boolean existsByToken(String token);
}
