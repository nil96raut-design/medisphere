package com.healthtrack.security;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Aspect
@Component
@Profile("!test")
public class RateLimitingAspect {

    private final Map<String, RateBucket> buckets = new ConcurrentHashMap<>();

    @Around("execution(* com.healthtrack.service.AuthService.login(..))")
    public Object limitLogin(ProceedingJoinPoint jp) throws Throwable {
        checkRate("login", 20, 60);
        return jp.proceed();
    }

    @Around("execution(* com.healthtrack.service.MockChatbotProvider.processChat(..))")
    public Object limitChatbot(ProceedingJoinPoint jp) throws Throwable {
        checkRate("chatbot", 10, 60);
        return jp.proceed();
    }

    @Around("execution(* com.healthtrack.service.BillingService.settle(..))")
    public Object limitBilling(ProceedingJoinPoint jp) throws Throwable {
        checkRate("billing", 10, 60);
        return jp.proceed();
    }

    private void checkRate(String key, int maxRequests, int windowSeconds) {
        RateBucket bucket = buckets.computeIfAbsent(key, k -> new RateBucket(maxRequests, windowSeconds));
        if (!bucket.tryConsume()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Rate limit exceeded for " + key + ". Max " + maxRequests + " requests per " + windowSeconds + "s");
        }
    }

    private static class RateBucket {
        private final int maxRequests;
        private final long windowMillis;
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart = System.currentTimeMillis();

        RateBucket(int maxRequests, int windowSeconds) {
            this.maxRequests = maxRequests;
            this.windowMillis = windowSeconds * 1000L;
        }

        synchronized boolean tryConsume() {
            long now = System.currentTimeMillis();
            if (now - windowStart > windowMillis) {
                count.set(0);
                windowStart = now;
            }
            if (count.get() >= maxRequests) return false;
            count.incrementAndGet();
            return true;
        }
    }
}
