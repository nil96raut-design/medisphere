package com.healthtrack.security;

import com.healthtrack.entity.SubscriptionStatus;
import com.healthtrack.entity.SubscriptionTier;
import com.healthtrack.util.DateUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SubscriptionGateFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionGateFilter.class);
    private static final int GRACE_PERIOD_DAYS = 3;

    private final com.healthtrack.repository.HospitalRepository hospitalRepository;

    public SubscriptionGateFilter(com.healthtrack.repository.HospitalRepository hospitalRepository) {
        this.hospitalRepository = hospitalRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                      @NonNull HttpServletResponse response,
                                      @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path.startsWith("/api/auth/") || path.startsWith("/api/chatbot/") || path.startsWith("/h2-console/")) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.debug("SubscriptionGate: path={}, auth={}, principalType={}", path, auth != null,
            auth != null ? auth.getPrincipal().getClass().getSimpleName() : "N/A");

        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            try {
                Long hospitalId = principal.getHospitalId();
                var hospital = hospitalId != null ? hospitalRepository.findById(hospitalId).orElse(null) : null;
                log.debug("SubscriptionGate: hospital loaded={}", hospital != null);
                if (hospital != null) {
                    SubscriptionStatus status = hospital.getSubscriptionStatus();
                    SubscriptionTier tier = hospital.getSubscriptionTier();
                    log.debug("SubscriptionGate: tier={}, status={}", tier, status);
                    var now = DateUtils.nowUtc();
                    var trialEnd = hospital.getTrialEndDate();
                    log.debug("SubscriptionGate: trialEnd={}, now={}", trialEnd, now);

                    boolean isTrialExpired = tier == SubscriptionTier.FREE_TRIAL
                            && trialEnd != null
                            && trialEnd.isBefore(now);

                    boolean isPastGracePeriod = status == SubscriptionStatus.EXPIRED
                            && trialEnd != null
                            && trialEnd.plusDays(GRACE_PERIOD_DAYS).isBefore(now);

                    log.debug("SubscriptionGate: isTrialExpired={}, isPastGracePeriod={}", isTrialExpired, isPastGracePeriod);

                    if (isTrialExpired || isPastGracePeriod) {
                        log.warn("SubscriptionGate: BLOCKING request - subscription not active");
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Hospital subscription is not active");
                        return;
                    }
                }
            } catch (Exception e) {
                log.error("SubscriptionGate: error checking subscription: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            }
        } else {
            log.debug("SubscriptionGate: no UserPrincipal auth found");
        }

        filterChain.doFilter(request, response);
    }
}
