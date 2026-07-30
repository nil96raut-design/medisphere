package com.healthtrack.service;

import com.healthtrack.repository.BedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PredictiveAnalyticsService {

    private final BedRepository bedRepository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public Map<String, Object> getPredictiveMetrics(Long hospitalId) {
        Map<String, Object> metrics = new HashMap<>();

        long totalBeds = bedRepository.count();
        long occupiedBeds = bedRepository.findByIsOccupiedFalse().size();
        long occupiedCount = totalBeds - occupiedBeds;

        double currentOccupancyRate = totalBeds > 0 ? ((double) occupiedCount / totalBeds) * 100 : 0.0;
        double forecastedOccupancy7Days = Math.min(100.0, currentOccupancyRate * 1.12); // 12% projected influx

        List<Map<String, Object>> doctorWorkload = jdbcTemplate.queryForList("""
            SELECT d.id as doctor_id, u.full_name, COUNT(a.id) as active_appointments
            FROM doctor d
            JOIN users u ON d.user_id = u.id
            LEFT JOIN appointment a ON a.doctor_id = d.id AND a.appointment_date = CURRENT_DATE
            WHERE d.hospital_id = ?
            GROUP BY d.id, u.full_name
            """, hospitalId);

        metrics.put("totalBeds", totalBeds);
        metrics.put("occupiedBeds", occupiedCount);
        metrics.put("currentOccupancyRatePercent", Math.round(currentOccupancyRate * 10.0) / 10.0);
        metrics.put("forecastedOccupancy7DaysPercent", Math.round(forecastedOccupancy7Days * 10.0) / 10.0);
        metrics.put("doctorWorkload", doctorWorkload);

        return metrics;
    }
}
