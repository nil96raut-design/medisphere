package com.healthtrack.security;

import com.healthtrack.entity.*;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;

@Aspect
@Component
public class TenantValidationAspect {

    @AfterReturning(pointcut = "execution(* com.healthtrack.service.*.*(..))", returning = "result")
    public void validateReturnedEntity(JoinPoint jp, Object result) {
        if (result == null) return;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth != null && auth.getPrincipal() instanceof UserPrincipal principal)) return;

        Long currentHospitalId = principal.getHospitalId();
        if (currentHospitalId == null) return;

        if (result instanceof Collection<?> collection) {
            for (Object item : collection) {
                validateEntity(item, currentHospitalId);
            }
        } else {
            validateEntity(result, currentHospitalId);
        }
    }

    private void validateEntity(Object entity, Long currentHospitalId) {
        Long entityHospitalId = null;

        if (entity instanceof User u) entityHospitalId = u.getHospital() != null ? u.getHospital().getId() : null;
        else if (entity instanceof Patient p) entityHospitalId = p.getHospital() != null ? p.getHospital().getId() : null;
        else if (entity instanceof Hospital h) entityHospitalId = h.getId();
        else if (entity instanceof MedicalRecord mr) entityHospitalId = mr.getHospital() != null ? mr.getHospital().getId() : null;
        else if (entity instanceof Appointment a) entityHospitalId = a.getHospital() != null ? a.getHospital().getId() : null;
        else if (entity instanceof Admission a) entityHospitalId = a.getHospital() != null ? a.getHospital().getId() : null;
        else if (entity instanceof Bill b) entityHospitalId = b.getHospital() != null ? b.getHospital().getId() : null;
        else if (entity instanceof BillingTransaction bt) entityHospitalId = bt.getHospital() != null ? bt.getHospital().getId() : null;
        else if (entity instanceof Bed b) entityHospitalId = b.getHospital() != null ? b.getHospital().getId() : null;
        else if (entity instanceof Doctor d) entityHospitalId = d.getHospital() != null ? d.getHospital().getId() : null;
        else if (entity instanceof DispensationRecord dr) entityHospitalId = dr.getHospital() != null ? dr.getHospital().getId() : null;
        else if (entity instanceof LabTestOrder lto) entityHospitalId = lto.getHospital() != null ? lto.getHospital().getId() : null;
        else if (entity instanceof MedicineStock ms) entityHospitalId = ms.getHospital() != null ? ms.getHospital().getId() : null;
        else if (entity instanceof NursingLog nl) entityHospitalId = nl.getHospital() != null ? nl.getHospital().getId() : null;
        else if (entity instanceof PrescriptionItem pi) entityHospitalId = pi.getHospital() != null ? pi.getHospital().getId() : null;
        else if (entity instanceof ProgressNote pn) entityHospitalId = pn.getHospital() != null ? pn.getHospital().getId() : null;
        else if (entity instanceof ServiceRequest sr) entityHospitalId = sr.getHospital() != null ? sr.getHospital().getId() : null;
        else if (entity instanceof Subscription s) entityHospitalId = s.getHospital() != null ? s.getHospital().getId() : null;
        else if (entity instanceof Task t) entityHospitalId = t.getHospital() != null ? t.getHospital().getId() : null;
        else if (entity instanceof Triage tr) entityHospitalId = tr.getHospital() != null ? tr.getHospital().getId() : null;

        if (entityHospitalId != null && !entityHospitalId.equals(currentHospitalId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Cross-tenant data leak blocked by AOP: " + entity.getClass().getSimpleName());
        }
    }
}
