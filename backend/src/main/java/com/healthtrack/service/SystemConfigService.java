package com.healthtrack.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private static final Logger log = LoggerFactory.getLogger(SystemConfigService.class);

    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    @Cacheable(value = "systemConfig", key = "#configKey")
    public String getConfig(String configKey, String defaultValue) {
        try {
            List<String> results = jdbcTemplate.query(
                    "SELECT setting_value FROM hospital_settings WHERE setting_key = ?",
                    (rs, rowNum) -> rs.getString("setting_value"),
                    configKey);

            return results.isEmpty() ? defaultValue : results.get(0);
        } catch (Exception e) {
            log.warn("Failed to fetch system config '{}', using default: {}", configKey, defaultValue);
            return defaultValue;
        }
    }

    @Transactional
    @CacheEvict(value = "systemConfig", key = "#configKey")
    public void setConfig(String configKey, String value) {
        jdbcTemplate.update("""
            INSERT INTO hospital_settings (setting_key, setting_value)
            VALUES (?, ?)
            ON CONFLICT (setting_key) DO UPDATE SET setting_value = EXCLUDED.setting_value
            """, configKey, value);
        log.info("System config '{}' updated to: {}", configKey, value);
    }
}
