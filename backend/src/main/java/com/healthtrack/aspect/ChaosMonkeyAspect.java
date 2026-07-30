package com.healthtrack.aspect;

import com.healthtrack.service.FeatureFlagService;
import lombok.Getter;
import lombok.Setter;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class ChaosMonkeyAspect {

    private static final Logger log = LoggerFactory.getLogger(ChaosMonkeyAspect.class);

    private final FeatureFlagService featureFlagService;

    @Getter
    @Setter
    private static volatile boolean dbOutageActive = false;

    @Getter
    @Setter
    private static volatile boolean redisOutageActive = false;

    @Getter
    @Setter
    private static volatile boolean kafkaDelayActive = false;

    private boolean isFlagActive(String flagName) {
        try {
            return featureFlagService.isFeatureEnabled(flagName);
        } catch (Exception e) {
            return false;
        }
    }

    @Around("execution(* com.healthtrack.repository..*(..)) && !execution(* com.healthtrack.repository.FeatureFlagRepository.*(..))")
    public Object interceptRepositoryCalls(ProceedingJoinPoint joinPoint) throws Throwable {
        if (dbOutageActive || isFlagActive("CHAOS_DB_OUTAGE")) {
            log.error("CHAOS MONKEY: Injecting DB Outage on {}", joinPoint.getSignature().toShortString());
            throw new DataAccessResourceFailureException("Chaos Monkey: Simulated DB Outage");
        }
        return joinPoint.proceed();
    }

    @Around("execution(* com.healthtrack.service..*(..)) && !execution(* com.healthtrack.service.FeatureFlagService.*(..))")
    public Object interceptServiceCalls(ProceedingJoinPoint joinPoint) throws Throwable {
        if (redisOutageActive || isFlagActive("CHAOS_REDIS_OUTAGE")) {
            if (joinPoint.getSignature().getName().toLowerCase().contains("redis") ||
                    joinPoint.getSignature().getDeclaringTypeName().contains("Cache")) {
                log.error("CHAOS MONKEY: Injecting Redis Failure on {}", joinPoint.getSignature().toShortString());
                throw new RedisConnectionFailureException("Chaos Monkey: Simulated Redis Failure");
            }
        }

        if (kafkaDelayActive || isFlagActive("CHAOS_KAFKA_DELAY")) {
            if (joinPoint.getSignature().getDeclaringTypeName().contains("Event") ||
                    joinPoint.getSignature().getName().toLowerCase().contains("publish")) {
                log.warn("CHAOS MONKEY: Injecting 3s Kafka/Event delay on {}", joinPoint.getSignature().toShortString());
                Thread.sleep(3000);
            }
        }

        return joinPoint.proceed();
    }
}
