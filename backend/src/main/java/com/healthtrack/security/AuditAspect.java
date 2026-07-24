package com.healthtrack.security;

import com.healthtrack.entity.AuditLog;
import com.healthtrack.repository.AuditLogRepository;
import com.healthtrack.util.DateUtils;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;

    @AfterReturning("execution(* com.healthtrack.service.BillingService.settle(..))")
    public void auditSettle(JoinPoint jp) {
        log("SETTLE_BILL", "Bill", null);
    }

    @AfterReturning("execution(* com.healthtrack.service.IpdService.admitPatient(..))")
    public void auditAdmit(JoinPoint jp) {
        log("ADMIT_PATIENT", "Admission", null);
    }

    @AfterReturning("execution(* com.healthtrack.service.IpdService.discharge(..))")
    public void auditDischarge(JoinPoint jp) {
        log("DISCHARGE_PATIENT", "Admission", null);
    }

    @AfterReturning("execution(* com.healthtrack.service.AuthService.register(..))")
    public void auditRegister(JoinPoint jp) {
        log("REGISTER_USER", "User", null);
    }

    @AfterReturning("execution(* com.healthtrack.service.AuthService.hospitalSignup(..))")
    public void auditHospitalSignup(JoinPoint jp) {
        log("HOSPITAL_SIGNUP", "Hospital", null);
    }

    @AfterReturning("execution(* com.healthtrack.service.AppointmentService.bookAppointment(..))")
    public void auditBookAppointment(JoinPoint jp) {
        log("BOOK_APPOINTMENT", "Appointment", null);
    }

    private void log(String action, String entity, Long entityId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
                auditLogRepository.save(AuditLog.builder()
                        .userId(principal.getUser().getId())
                        .hospitalId(principal.getHospitalId())
                        .action(action)
                        .entity(entity)
                        .entityId(entityId)
                        .timestamp(DateUtils.nowUtc())
                        .build());
            }
        } catch (Exception ignored) {
        }
    }
}
