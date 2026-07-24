package com.healthtrack.service;

import com.healthtrack.dto.LabDtos.*;
import com.healthtrack.entity.*;
import com.healthtrack.repository.LabTestOrderRepository;
import com.healthtrack.repository.PatientRepository;
import com.healthtrack.security.TenantValidator;
import com.healthtrack.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LabService {

    private final LabTestOrderRepository labTestOrderRepository;
    private final PatientRepository patientRepository;
    private final TenantValidator tenantValidator;

    @Transactional(readOnly = true)
    public List<LabOrderResponse> getOrdersByStatus(String status, int page, int size, UserPrincipal currentUser) {
        String s = (status == null || status.isBlank()) ? "ORDERED" : status;
        LabOrderStatus orderStatus;
        try {
            orderStatus = LabOrderStatus.valueOf(s);
        } catch (IllegalArgumentException e) {
            orderStatus = LabOrderStatus.ORDERED;
        }
        return labTestOrderRepository.findByStatusOrderByCreatedAtDesc(orderStatus).stream()
                .skip((long) page * size)
                .limit(size)
                .map(this::mapToResponse).toList();
    }

    @Transactional
    public LabOrderResponse markSampleCollected(Long orderId, SampleCollectionRequest request, UserPrincipal currentUser) {
        User user = currentUser.getUser();
        if (user.getRole() != Role.LAB_TECH && user.getRole() != Role.DOCTOR && user.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only lab technicians can collect samples");
        }

        LabTestOrder order = labTestOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab order not found"));
        tenantValidator.validateHospitalAccess(order.getHospital().getId(), currentUser.getHospitalId());

        if (order.getStatus() != LabOrderStatus.ORDERED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sample already collected for this order");
        }

        order.setStatus(LabOrderStatus.SAMPLE_COLLECTED);
        order.setTechnicianNotes(request.technicianNotes());
        order = labTestOrderRepository.save(order);
        return mapToResponse(order);
    }

    @Transactional
    public LabOrderResponse enterResults(Long orderId, ResultEntryRequest request, UserPrincipal currentUser) {
        User user = currentUser.getUser();
        if (user.getRole() != Role.LAB_TECH && user.getRole() != Role.DOCTOR && user.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only lab technicians can enter results");
        }

        LabTestOrder order = labTestOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab order not found"));
        tenantValidator.validateHospitalAccess(order.getHospital().getId(), currentUser.getHospitalId());

        if (order.getStatus() == LabOrderStatus.RESULT_READY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Results already entered for this order");
        }

        order.setStatus(LabOrderStatus.RESULT_READY);
        order.setResultValues(request.resultValues());
        if (request.technicianNotes() != null) {
            order.setTechnicianNotes(request.technicianNotes());
        }
        order.setCompletedAt(LocalDateTime.now());
        order = labTestOrderRepository.save(order);
        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public LabOrderResponse getOrder(Long orderId, UserPrincipal currentUser) {
        LabTestOrder order = labTestOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab order not found"));
        tenantValidator.validateHospitalAccess(order.getHospital().getId(), currentUser.getHospitalId());
        return mapToResponse(order);
    }

    private LabOrderResponse mapToResponse(LabTestOrder o) {
        return new LabOrderResponse(
                o.getId(),
                o.getPatient().getId(),
                o.getPatient().getFirstName() + " " + o.getPatient().getLastName(),
                o.getTestName(),
                o.getRequestedBy().getFullName(),
                o.getStatus().name(),
                o.getResultValues(),
                o.getTechnicianNotes(),
                o.getCompletedAt(),
                o.getCreatedAt());
    }
}
