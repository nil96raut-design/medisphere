package com.healthtrack.aspect;

import com.healthtrack.dto.PatientDtos.PatientResponse;
import com.healthtrack.security.UserPrincipal;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

@Aspect
@Component
public class PiiMaskingAspect {

    private static final Logger log = LoggerFactory.getLogger(PiiMaskingAspect.class);

    @Around("execution(* com.healthtrack.controller.PatientController.*(..)) || execution(* com.healthtrack.controller.v1.V1PatientController.*(..))")
    public Object maskPatientPii(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal currentUser) {
            String roleName = currentUser.getUser().getRole().name();
            
            // Administrative & Doctor roles see unmasked PII; lower-tier roles get masked PII
            if (!"ADMIN".equals(roleName) && !"DOCTOR".equals(roleName)) {
                log.debug("Masking PII fields for role: {}", roleName);
                if (result instanceof ResponseEntity<?> responseEntity) {
                    Object body = responseEntity.getBody();
                    if (body instanceof PatientResponse p) {
                        PatientResponse masked = maskResponse(p);
                        return ResponseEntity.status(responseEntity.getStatusCode()).headers(responseEntity.getHeaders()).body(masked);
                    } else if (body instanceof List<?> list) {
                        List<?> maskedList = list.stream().map(item -> {
                            if (item instanceof PatientResponse p) {
                                return maskResponse(p);
                            }
                            return item;
                        }).toList();
                        return ResponseEntity.status(responseEntity.getStatusCode()).headers(responseEntity.getHeaders()).body(maskedList);
                    }
                }
            }
        }

        return result;
    }

    private PatientResponse maskResponse(PatientResponse p) {
        String maskedPhone = p.phoneNumber() != null && p.phoneNumber().length() > 4 ?
                "***-***-" + p.phoneNumber().substring(p.phoneNumber().length() - 4) : "****";
        String maskedEmail = p.email() != null && p.email().contains("@") ?
                "***@" + p.email().split("@")[1] : "***@***.com";

        return new PatientResponse(
                p.id(), p.firstName(), p.lastName(), p.gender(), p.dateOfBirth(),
                maskedPhone, maskedEmail, p.emergencyContact(), p.policyNumber(),
                p.degraded(), p.lastUpdated()
        );
    }
}
