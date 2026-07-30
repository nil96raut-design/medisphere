package com.healthtrack.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    private final StringRedisTemplate redisTemplate;

    private static final int MAX_REQUESTS_PER_MINUTE = 120;
    private static final long WINDOW_SIZE_SECONDS = 60;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = getClientIp(request);
        String redisKey = "rate_limit:" + clientIp;

        try {
            long currentTimeMillis = System.currentTimeMillis();
            long windowStart = currentTimeMillis - (WINDOW_SIZE_SECONDS * 1000);

            // Redis ZSET sliding window
            redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, windowStart);
            Long requestCount = redisTemplate.opsForZSet().zCard(redisKey);

            if (requestCount != null && requestCount >= MAX_REQUESTS_PER_MINUTE) {
                log.warn("Rate limit exceeded for IP: {} (Count: {})", clientIp, requestCount);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("{\"error\": \"Rate limit exceeded. Please try again later.\"}");
                return;
            }

            redisTemplate.opsForZSet().add(redisKey, String.valueOf(currentTimeMillis), currentTimeMillis);
            redisTemplate.expire(redisKey, WINDOW_SIZE_SECONDS, TimeUnit.SECONDS);

        } catch (Exception e) {
            log.warn("Redis rate limiter unavailable, failing OPEN: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
