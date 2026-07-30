package com.healthtrack.service;

import com.healthtrack.dto.ClinicalSafetyDtos.*;
import com.healthtrack.entity.*;
import com.healthtrack.event.EventConstants;
import com.healthtrack.event.EventPublisher;
import com.healthtrack.repository.*;
import com.healthtrack.security.TenantValidator;
import com.healthtrack.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShiftHandoverService {

    private final ShiftHandoverRepository shiftHandoverRepository;
    private final UserRepository userRepository;
    private final NurseAssignmentRepository nurseAssignmentRepository;
    private final NurseTaskRepository nurseTaskRepository;
    private final AlertRepository alertRepository;
    private final TenantValidator tenantValidator;
    private final EventPublisher eventPublisher;

    @Transactional
    public ShiftHandoverResponse submitHandover(ShiftHandoverRequest request, UserPrincipal currentUser) {
        Long hospitalId = currentUser.getHospitalId();
        User fromNurse = currentUser.getUser();

        if (fromNurse.getRole() != Role.NURSE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only nurses can submit handovers");
        }

        User toNurse = userRepository.findById(request.toNurseId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target nurse not found"));
        tenantValidator.validateHospitalAccess(toNurse.getHospital().getId(), hospitalId);

        if (toNurse.getRole() != Role.NURSE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target user is not a nurse");
        }

        List<NurseAssignment> activeAssignments = nurseAssignmentRepository
                .findByNurseIdAndStatusFetching(fromNurse.getId(), NurseAssignmentStatus.ACTIVE);

        String patientSummary = activeAssignments.stream()
                .map(a -> {
                    long pendingTasks = nurseTaskRepository
                            .findByPatientIdOrderByCreatedAtDesc(a.getPatient().getId()).stream()
                            .filter(t -> t.getStatus() != NurseTaskStatus.DONE)
                            .count();
                    long activeAlerts = alertRepository
                            .countByPatientIdAndStatusIn(a.getPatient().getId(),
                                    List.of(AlertStatus.ACTIVE, AlertStatus.ACKNOWLEDGED, AlertStatus.ESCALATED));
                    return String.format("- %s %s (Bed: %s, Tasks: %d, Alerts: %d)",
                            a.getPatient().getFirstName(), a.getPatient().getLastName(),
                            a.getBed() != null ? a.getBed().getBedNumber() : "N/A",
                            pendingTasks, activeAlerts);
                })
                .collect(Collectors.joining("\n"));

        for (NurseAssignment assignment : activeAssignments) {
            assignment.setNurse(toNurse);
        }
        nurseAssignmentRepository.saveAll(activeAssignments);

        ShiftHandover handover = ShiftHandover.builder()
                .hospital(fromNurse.getHospital())
                .fromNurse(fromNurse)
                .toNurse(toNurse)
                .wardName(request.wardName())
                .notes(request.notes())
                .patientSummary(patientSummary)
                .build();
        handover = shiftHandoverRepository.save(handover);

        eventPublisher.publish(EventConstants.SHIFT_HANDOVER_COMPLETED, hospitalId,
                Map.of("handoverId", handover.getId(), "fromNurseId", fromNurse.getId(),
                        "toNurseId", request.toNurseId(), "patientCount", activeAssignments.size(),
                        "hospitalId", hospitalId));

        return mapHandoverResponse(handover);
    }

    @Transactional(readOnly = true)
    public List<ShiftHandoverResponse> getMyHandovers(UserPrincipal currentUser) {
        Long nurseId = currentUser.getUser().getId();
        return shiftHandoverRepository.findByFromNurseIdOrderByCreatedAtDesc(nurseId).stream()
                .map(this::mapHandoverResponse).toList();
    }

    private ShiftHandoverResponse mapHandoverResponse(ShiftHandover h) {
        return new ShiftHandoverResponse(
                h.getId(), h.getFromNurse().getId(), h.getFromNurse().getFullName(),
                h.getToNurse().getId(), h.getToNurse().getFullName(),
                h.getWardName(), h.getNotes(), h.getPatientSummary(), h.getCreatedAt());
    }
}
