package com.healthtrack.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import org.springframework.data.redis.core.StringRedisTemplate;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.redis.RedisConnectionFailureException;
import com.healthtrack.repository.TokenBlocklistRepository;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;
    private final StringRedisTemplate redisTemplate;
    private final MeterRegistry meterRegistry;
    private final TokenBlocklistRepository tokenBlocklistRepository;
    
    private final ConcurrentHashMap<String, Boolean> localBlocklist = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        String jwt = null;
        
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("ht_access_token".equals(cookie.getName())) {
                    jwt = cookie.getValue();
                    break;
                }
            }
        }

        final String authHeader = request.getHeader("Authorization");
        if (jwt == null && authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
        }
        
        log.debug("JwtAuthFilter processing request: {} {}, token present: {}", request.getMethod(), request.getRequestURI(), jwt != null);

        if (jwt == null) {
            log.debug("No token found in cookies or header, skipping JWT auth");
            filterChain.doFilter(request, response);
            return;
        }
        log.debug("JWT token length: {}", jwt.length());

        try {
            boolean isBlocked = localBlocklist.getOrDefault(jwt, false);
            if (!isBlocked) {
                try {
                    isBlocked = Boolean.TRUE.equals(redisTemplate.hasKey("blocklist:" + jwt));
                } catch (RedisConnectionFailureException e) {
                    log.warn("Redis unavailable during JWT blocklist check. Failing OPEN. Error: {}", e.getMessage());
                    meterRegistry.counter("auth.redis.failures.count").increment();
                } catch (Exception e) {
                    log.warn("Error checking Redis blocklist. Failing OPEN. Error: {}", e.getMessage());
                }
            }

            if (!isBlocked) {
                try {
                    isBlocked = tokenBlocklistRepository.existsByToken(jwt);
                    if (isBlocked) {
                        try {
                            long expirationTime = jwtService.getExpiration(jwt).getTime();
                            long ttl = expirationTime - System.currentTimeMillis();
                            if (ttl > 0) {
                                redisTemplate.opsForValue().set("blocklist:" + jwt, "true", ttl, java.util.concurrent.TimeUnit.MILLISECONDS);
                            }
                        } catch (Exception redisEx) {
                            log.warn("Failed to backfill Redis blocklist cache: {}", redisEx.getMessage());
                        }
                    }
                } catch (Exception e) {
                    log.warn("Error checking DB blocklist. Failing OPEN. Error: {}", e.getMessage());
                }
            }

            if (isBlocked) {
                log.warn("Token is blocklisted");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            final String userEmail = jwtService.extractUsername(jwt);
            log.debug("Extracted username from token: {}", userEmail);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
                log.debug("Loaded UserDetails: {} with authorities: {}", userDetails.getUsername(), userDetails.getAuthorities());

                boolean valid = jwtService.isTokenValid(jwt, userDetails);
                log.debug("Token valid: {}", valid);

                if (valid) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("Authentication set for user: {}", userEmail);
                } else {
                    log.warn("Token is NOT valid for user: {}", userEmail);
                }
            } else {
                log.debug("userEmail null or auth already set; userEmail={}, existingAuth={}",
                    userEmail, SecurityContextHolder.getContext().getAuthentication());
            }
        } catch (Exception e) {
            log.error("JWT processing failed: {} - {}", e.getClass().getSimpleName(), e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
