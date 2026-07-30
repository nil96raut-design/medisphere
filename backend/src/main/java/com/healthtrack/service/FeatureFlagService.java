package com.healthtrack.service;

import com.healthtrack.entity.FeatureFlag;
import com.healthtrack.repository.FeatureFlagRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FeatureFlagService {

    private static final Logger log = LoggerFactory.getLogger(FeatureFlagService.class);

    private final FeatureFlagRepository featureFlagRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "featureFlags", key = "#featureName")
    public boolean isFeatureEnabled(String featureName) {
        return featureFlagRepository.findByFeatureName(featureName)
                .map(FeatureFlag::getIsEnabled)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<FeatureFlag> getAllFeatureFlags() {
        return featureFlagRepository.findAll();
    }

    @Transactional
    @CacheEvict(value = "featureFlags", key = "#featureName")
    public FeatureFlag toggleFeature(String featureName, boolean isEnabled) {
        FeatureFlag flag = featureFlagRepository.findByFeatureName(featureName)
                .orElseGet(() -> FeatureFlag.builder()
                        .featureName(featureName)
                        .isEnabled(isEnabled)
                        .description("Dynamic feature flag")
                        .build());

        flag.setIsEnabled(isEnabled);
        FeatureFlag saved = featureFlagRepository.save(flag);
        log.info("Feature flag '{}' set to: {}", featureName, isEnabled);
        return saved;
    }
}
