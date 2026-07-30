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

    @AfterReturning("execution(* com.healthtrack.service.NurseService.recordVitals(..))")
    public void auditRecordVitals(JoinPoint jp) {
        log("RECORD_VITALS", "VitalRecord", null);
    }

    @AfterReturning("execution(* com.healthtrack.service.NurseService.administerMedication(..))")
    public void auditAdministerMedication(JoinPoint jp) {
        log("ADMINISTER_MEDICATION", "MedicationAdministration", null);
    }

    @AfterReturning("execution(* com.healthtrack.service.NurseService.addNursingNote(..))")
    public void auditAddNursingNote(JoinPoint jp) {
        log("ADD_NURSING_NOTE", "NursingNote", null);
    }

    @AfterReturning("execution(* com.healthtrack.service.NurseService.updateTaskStatus(..))")
    public void auditUpdateTaskStatus(JoinPoint jp) {
        log("UPDATE_NURSE_TASK", "NurseTask", null);
    }

    @AfterReturning("execution(* com.healthtrack.service.AlertService.acknowledgeAlert(..))")
    public void auditAcknowledgeAlert(JoinPoint jp) {
        log("ACKNOWLEDGE_ALERT", "Alert", null);
    }

    @AfterReturning("execution(* com.healthtrack.service.AlertService.resolveAlert(..))")
    public void auditResolveAlert(JoinPoint jp) {
        log("RESOLVE_ALERT", "Alert", null);
    }

    @AfterReturning("execution(* com.healthtrack.service.ShiftHandoverService.submitHandover(..))")
    public void auditSubmitHandover(JoinPoint jp) {
        log("SUBMIT_HANDOVER", "ShiftHandover", null);
    }

    @AfterReturning("execution(* com.healthtrack.service.BedCleaningService.requestCleaning(..))")
    public void auditRequestCleaning(JoinPoint jp) {
        log("REQUEST_CLEANING", "BedCleaningRequest", null);
    }

    @AfterReturning("execution(* com.healthtrack.service.BedCleaningService.markCleaned(..))")
    public void auditMarkCleaned(JoinPoint jp) {
        log("MARK_CLEANED", "BedCleaningRequest", null);
    }

    @AfterReturning("execution(* com.healthtrack.service.PharmacyService.dispense(..))")
    public void auditDispense(JoinPoint jp) {
        log("DISPENSE_MEDICINE", "DispensationRecord", null);
    }

    @AfterReturning("execution(* com.healthtrack.service.SupplierService.createPurchaseOrder(..))")
    public void auditReorder(JoinPoint jp) {
        log("REORDER_CREATED", "PurchaseOrder", null);
    }

    @AfterReturning("execution(* com.healthtrack.service.BillingService.refund(..))")
    public void auditRefund(JoinPoint jp) {
        log("REFUND_BILL", "Bill", null);
    }

    @AfterReturning("execution(* com.healthtrack.service.LabService.markSampleCollected(..))")
    public void auditLabSampleCollected(JoinPoint jp) {
        log("LAB_SAMPLE_COLLECTED", "LabTestOrder", null);
    }

    @AfterReturning("execution(* com.healthtrack.service.LabService.startProcessing(..))")
    public void auditLabProcessing(JoinPoint jp) {
        log("LAB_PROCESSING", "LabTestOrder", null);
    }

    @AfterReturning("execution(* com.healthtrack.service.LabService.enterResults(..))")
    public void auditLabResultEntered(JoinPoint jp) {
        log("LAB_RESULT_ENTERED", "LabTestOrder", null);
    }

    @AfterReturning("execution(* com.healthtrack.service.LabService.requestRetest(..))")
    public void auditLabRetest(JoinPoint jp) {
        log("LAB_RETEST_REQUESTED", "LabTestOrder", null);
    }

    @AfterReturning("execution(* com.healthtrack.service.LabService.approve(..))")
    public void auditLabApprove(JoinPoint jp) {
        log("LAB_APPROVED", "LabTestOrder", null);
    }

    private void log(String action, String entity, Long entityId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
                String correlationId = org.slf4j.MDC.get("correlationId");
                auditLogRepository.save(AuditLog.builder()
                        .userId(principal.getUser().getId())
                        .hospitalId(principal.getHospitalId())
                        .action(action)
                        .entity(entity)
                        .entityId(entityId)
                        .timestamp(DateUtils.nowUtc())
                        .correlationId(correlationId)
                        .build());
            }
        } catch (Exception ignored) {
        }
    }
}
