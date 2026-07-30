package com.healthtrack.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GlobalSearchService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public Map<String, Object> globalSearch(String query, Long hospitalId) {
        Map<String, Object> results = new HashMap<>();

        if (query == null || query.trim().isEmpty()) {
            return results;
        }

        String searchPattern = "%" + query.trim().toLowerCase() + "%";

        // Patients FTS
        List<Map<String, Object>> patients = jdbcTemplate.queryForList("""
            SELECT id, first_name, last_name, phone_number, gender 
            FROM patients 
            WHERE hospital_id = ? AND (LOWER(first_name) LIKE ? OR LOWER(last_name) LIKE ? OR phone_number LIKE ?)
            LIMIT 10
            """, hospitalId, searchPattern, searchPattern, searchPattern);

        // Lab Orders Search
        List<Map<String, Object>> labOrders = jdbcTemplate.queryForList("""
            SELECT id, test_name, status, created_at 
            FROM lab_test_order 
            WHERE hospital_id = ? AND LOWER(test_name) LIKE ?
            LIMIT 10
            """, hospitalId, searchPattern);

        results.put("patients", patients);
        results.put("labOrders", labOrders);
        results.put("query", query);
        return results;
    }
}
