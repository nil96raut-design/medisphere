package com.healthtrack.service;

import com.healthtrack.dto.LabDtos.*;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LabService {

    private final LabTestOrderRepository labTestOrderRepository;
    private final LabCriticalRuleRepository labCriticalRuleRepository;
    private final LabResultHistoryRepository labResultHistoryRepository;
    private final SampleTrackingRepository sampleTrackingRepository;
    private final SlaBreachRepository slaBreachRepository;
    private final AlertRepository alertRepository;
    private final PatientRepository patientRepository;
    private final TenantValidator tenantValidator;
    private final EventPublisher eventPublisher;
    private final IdempotencyService idempotencyService;

    @Transactional(readOnly = true)
    public LabTechQueueResponse getTechQueue(UserPrincipal currentUser) {
        Long hospitalId = currentUser.getHospitalId();
        List<LabOrderStatus> activeStatuses = List.of(
                LabOrderStatus.ORDERED, LabOrderStatus.SAMPLE_COLLECTED,
                LabOrderStatus.PROCESSING, LabOrderStatus.RESULT_ENTERED,
                LabOrderStatus.NEEDS_RETEST);
        List<LabTestOrder> active = labTestOrderRepository
                .findByHospitalIdAndStatusInOrderByCreatedAtDesc(hospitalId, activeStatuses);

        List<LabOrderListResponse> pendingCollection = new ArrayList<>();
        List<LabOrderListResponse> inProcessing = new ArrayList<>();
        List<LabOrderListResponse> pendingResults = new ArrayList<>();
        List<LabOrderListResponse> criticalResults = new ArrayList<>();

        for (LabTestOrder o : active) {
            LabOrderListResponse r = mapToListResponse(o);
            switch (o.getStatus()) {
                case ORDERED -> pendingCollection.add(r);
                case SAMPLE_COLLECTED -> inProcessing.add(r);
                case PROCESSING -> inProcessing.add(r);
                case RESULT_ENTERED, NEEDS_RETEST -> pendingResults.add(r);
            }
            if (Boolean.TRUE.equals(o.getCriticalFlag())) {
                criticalResults.add(r);
            }
        }

        LabMetricsResponse metrics = computeMetrics(hospitalId);
        return new LabTechQueueResponse(pendingCollection, inProcessing, pendingResults, criticalResults, metrics);
    }

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
        if (user.getRole() != Role.LAB_TECH && user.getRole() != Role.NURSE && user.getRole() != Role.DOCTOR && user.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only lab techs or nurses can collect samples");
        }

        LabTestOrder order = findOrderAndValidate(orderId, currentUser);

        if (order.getStatus() != LabOrderStatus.ORDERED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only ORDERED orders can have samples collected (current: " + order.getStatus() + ")");
        }

        order.setStatus(LabOrderStatus.SAMPLE_COLLECTED);
        order.setTechnicianNotes(request.technicianNotes());
        order.setSampleCollectedAt(LocalDateTime.now());
        order.setSampleBarcode(request.barcode());
        order.setSampleStorageLocation(request.storageLocation());

        if (request.barcode() != null || request.sampleType() != null) {
            SampleTracking tracking = SampleTracking.builder()
                    .hospital(order.getHospital())
                    .labOrder(order)
                    .sampleType(request.sampleType() != null ? request.sampleType() : "Blood")
                    .containerType(request.containerType())
                    .barcode(request.barcode())
                    .collectionVolume(request.collectionVolume())
                    .collectionMethod(request.collectionMethod())
                    .storageLocation(request.storageLocation())
                    .storageCondition(request.storageCondition())
                    .collectedBy(user)
                    .notes(request.technicianNotes())
                    .build();
            sampleTrackingRepository.save(tracking);
            order.getSampleTrackings().add(tracking);
        }

        order = labTestOrderRepository.save(order);
        return mapToResponse(order);
    }

    @Transactional
    public LabOrderResponse startProcessing(Long orderId, ProcessRequest request, UserPrincipal currentUser) {
        User user = currentUser.getUser();
        if (user.getRole() != Role.LAB_TECH && user.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only lab techs can process samples");
        }

        LabTestOrder order = findOrderAndValidate(orderId, currentUser);

        if (order.getStatus() != LabOrderStatus.SAMPLE_COLLECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only SAMPLE_COLLECTED orders can be processed (current: " + order.getStatus() + ")");
        }

        order.setStatus(LabOrderStatus.PROCESSING);
        order.setProcessingStartedAt(LocalDateTime.now());
        if (request.technicianNotes() != null) {
            order.setTechnicianNotes(request.technicianNotes());
        }

        order = labTestOrderRepository.save(order);
        return mapToResponse(order);
    }

    @Transactional
    public LabOrderResponse enterResults(Long orderId, ResultEntryRequest request, UserPrincipal currentUser) {
        User user = currentUser.getUser();
        if (user.getRole() != Role.LAB_TECH && user.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only lab techs can enter results");
        }

        String requestId = "ENTER_RES_" + orderId + "_" + (request.resultValues() != null ? request.resultValues().hashCode() : 0);
        if (!idempotencyService.tryProcess(requestId, currentUser.getHospitalId(), "ENTER_LAB_RESULT")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate result entry detected");
        }

        LabTestOrder order = findOrderAndValidate(orderId, currentUser);

        if (order.getStatus() == LabOrderStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Results already entered for this order (current: " + order.getStatus() + ")");
        }

        if (order.getStatus() != LabOrderStatus.PROCESSING && order.getStatus() != LabOrderStatus.SAMPLE_COLLECTED
                && order.getStatus() != LabOrderStatus.NEEDS_RETEST && order.getStatus() != LabOrderStatus.RESULT_ENTERED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot enter results in status " + order.getStatus());
        }

        // Versioning: deactivate previous active version, create new version
        labResultHistoryRepository.findByLabOrderIdAndIsActiveTrue(order.getId())
                .ifPresent(prev -> {
                    prev.setIsActive(false);
                    labResultHistoryRepository.save(prev);
                });

        int nextVersion = labResultHistoryRepository.countByLabOrderId(order.getId()) + 1;

        LabResultHistory history = LabResultHistory.builder()
                .labOrder(order)
                .version(nextVersion)
                .resultData(request.resultValues())
                .createdBy(user)
                .isActive(true)
                .build();
        labResultHistoryRepository.save(history);

        order.setResultValues(request.resultValues());
        order.setResultEnteredBy(user);
        order.setProcessingCompletedAt(LocalDateTime.now());

        if (request.technicianNotes() != null) {
            order.setTechnicianNotes(request.technicianNotes());
        }

        if (order.getSampleCollectedAt() != null) {
            int minutes = (int) ChronoUnit.MINUTES.between(order.getSampleCollectedAt(), LocalDateTime.now());
            order.setTurnaroundMinutes(minutes);
        }

        order.setStatus(LabOrderStatus.RESULT_ENTERED);

        boolean isCritical = detectCriticalResult(order, currentUser.getHospitalId());
        if (isCritical) {
            order.setCriticalFlag(true);
            eventPublisher.publishAsync(EventConstants.CRITICAL_LAB_RESULT, currentUser.getHospitalId(),
                    Map.of("orderId", order.getId(), "patientId", order.getPatient().getId(),
                            "testName", order.getTestName()));
            createLabAlert(order, isCritical, currentUser);
        }

        order = labTestOrderRepository.save(order);

        checkSlaBreach(order.getSampleCollectedAt(), order.getTestName(), order, currentUser);

        eventPublisher.publishAsync(EventConstants.LAB_RESULT_READY, currentUser.getHospitalId(),
                Map.of("orderId", order.getId(), "patientId", order.getPatient().getId(),
                        "testName", order.getTestName()));

        return mapToResponse(order);
    }

    @Transactional
    public LabOrderResponse requestRetest(Long orderId, RetestRequest request, UserPrincipal currentUser) {
        User user = currentUser.getUser();
        if (user.getRole() != Role.LAB_TECH && user.getRole() != Role.DOCTOR && user.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only lab techs or doctors can request retests");
        }

        LabTestOrder order = findOrderAndValidate(orderId, currentUser);

        if (order.getStatus() != LabOrderStatus.RESULT_ENTERED && order.getStatus() != LabOrderStatus.PENDING_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Retest can only be requested for RESULT_ENTERED or PENDING_APPROVAL orders (current: " + order.getStatus() + ")");
        }

        order.setStatus(LabOrderStatus.NEEDS_RETEST);
        order.setCorrectionReason(request.correctionReason());
        order = labTestOrderRepository.save(order);
        return mapToResponse(order);
    }

    @Transactional
    public LabOrderResponse approve(Long orderId, ApproveRequest request, UserPrincipal currentUser) {
        User user = currentUser.getUser();
        if (user.getRole() != Role.DOCTOR && user.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only doctors can approve lab results");
        }

        LabTestOrder order = findOrderAndValidate(orderId, currentUser);

        if (order.getStatus() != LabOrderStatus.RESULT_ENTERED && order.getStatus() != LabOrderStatus.PENDING_APPROVAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only RESULT_ENTERED or PENDING_APPROVAL orders can be approved (current: " + order.getStatus() + ")");
        }

        order.setStatus(LabOrderStatus.APPROVED);
        order.setApprovedBy(user);
        order.setApprovedAt(java.time.OffsetDateTime.now());
        if (order.getCompletedAt() == null) {
            order.setCompletedAt(LocalDateTime.now());
        }
        order = labTestOrderRepository.save(order);
        return mapToResponse(order);
    }

    @Transactional
    public LabOrderResponse cancel(Long orderId, UserPrincipal currentUser) {
        User user = currentUser.getUser();
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.LAB_TECH) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins or lab techs can cancel orders");
        }

        LabTestOrder order = findOrderAndValidate(orderId, currentUser);

        if (order.getStatus() == LabOrderStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot cancel an approved order");
        }

        order.setStatus(LabOrderStatus.CANCELLED);
        order = labTestOrderRepository.save(order);
        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public LabOrderResponse getOrder(Long orderId, UserPrincipal currentUser) {
        User user = currentUser.getUser();
        LabTestOrder order = labTestOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab order not found"));
        tenantValidator.validateHospitalAccess(order.getHospital().getId(), currentUser.getHospitalId());

        if (user.getRole() == Role.DOCTOR && order.getStatus() != LabOrderStatus.APPROVED && order.getStatus() != LabOrderStatus.RESULT_ENTERED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Lab results must be entered before they can be viewed by doctors");
        }

        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public LabMetricsResponse getMetrics(UserPrincipal currentUser) {
        return computeMetrics(currentUser.getHospitalId());
    }

    @Transactional(readOnly = true)
    public List<CriticalRuleResponse> getCriticalRules(UserPrincipal currentUser) {
        return labCriticalRuleRepository.findByHospitalId(currentUser.getHospitalId()).stream()
                .map(r -> new CriticalRuleResponse(r.getId(), r.getTestName(), r.getParameterName(),
                        r.getConditionOperator(), r.getThresholdValue(), r.getUnit(), r.getSeverity(), r.getEnabled()))
                .toList();
    }

    @Transactional
    public CriticalRuleResponse createCriticalRule(CriticalRuleRequest request, UserPrincipal currentUser) {
        LabCriticalRule rule = LabCriticalRule.builder()
                .hospital(currentUser.getUser().getHospital())
                .testName(request.testName())
                .parameterName(request.parameterName())
                .conditionOperator(request.conditionOperator())
                .thresholdValue(request.thresholdValue())
                .unit(request.unit())
                .severity(request.severity() != null ? request.severity() : "HIGH")
                .build();
        rule = labCriticalRuleRepository.save(rule);
        return new CriticalRuleResponse(rule.getId(), rule.getTestName(), rule.getParameterName(),
                rule.getConditionOperator(), rule.getThresholdValue(), rule.getUnit(), rule.getSeverity(), rule.getEnabled());
    }

    @Transactional
    public void deleteCriticalRule(Long ruleId, UserPrincipal currentUser) {
        LabCriticalRule rule = labCriticalRuleRepository.findByIdAndHospitalId(ruleId, currentUser.getHospitalId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Critical rule not found"));
        labCriticalRuleRepository.delete(rule);
    }

    @Transactional
    public CriticalRuleResponse toggleCriticalRule(Long ruleId, UserPrincipal currentUser) {
        LabCriticalRule rule = labCriticalRuleRepository.findByIdAndHospitalId(ruleId, currentUser.getHospitalId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Critical rule not found"));
        rule.setEnabled(!rule.getEnabled());
        rule = labCriticalRuleRepository.save(rule);
        return new CriticalRuleResponse(rule.getId(), rule.getTestName(), rule.getParameterName(),
                rule.getConditionOperator(), rule.getThresholdValue(), rule.getUnit(), rule.getSeverity(), rule.getEnabled());
    }

    // ──────────────────────────────────────────────
    // LAB RESULT VERSIONING
    // ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ResultHistoryResponse> getResultHistory(Long orderId, UserPrincipal currentUser) {
        findOrderAndValidate(orderId, currentUser);
        return labResultHistoryRepository.findByLabOrderIdOrderByVersionDesc(orderId).stream()
                .map(h -> new ResultHistoryResponse(h.getId(), h.getLabOrder().getId(), h.getVersion(),
                        h.getResultData(), h.getCreatedBy().getFullName(), h.getIsActive(), h.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ResultHistoryResponse> compareResults(Long orderId, UserPrincipal currentUser) {
        return getResultHistory(orderId, currentUser);
    }

    // ──────────────────────────────────────────────
    // LAB TREND ANALYTICS
    // ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<LabTrendResponse> getPatientTrends(Long patientId, UserPrincipal currentUser) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
        tenantValidator.validateHospitalAccess(patient.getHospital().getId(), currentUser.getHospitalId());

        List<LabTestOrder> orders = labTestOrderRepository.findByPatientIdOrderByCreatedAtDesc(patientId).stream()
                .filter(o -> o.getResultValues() != null && !o.getResultValues().isBlank()
                        && (o.getStatus() == LabOrderStatus.RESULT_ENTERED || o.getStatus() == LabOrderStatus.APPROVED))
                .toList();

        Map<String, List<TrendDataPoint>> trendMap = new LinkedHashMap<>();
        for (LabTestOrder o : orders) {
            String testName = o.getTestName();
            String[] lines = o.getResultValues().split("\n");
            for (String line : lines) {
                if (line.contains(":") || line.contains("=")) {
                    String[] parts = line.split("[:=]");
                    if (parts.length >= 2) {
                        String param = parts[0].trim();
                        String valStr = parts[1].trim();
                        String unit = "";
                        String[] valParts = valStr.split("\\s+");
                        String value = valParts[0];
                        if (valParts.length > 1) unit = valParts[1];
                        String key = testName + " - " + param;
                        trendMap.computeIfAbsent(key, k -> new ArrayList<>())
                                .add(new TrendDataPoint(o.getCreatedAt().toString(), value, unit, ""));
                    }
                }
            }
        }

        return trendMap.entrySet().stream()
                .map(e -> {
                    String[] parts = e.getKey().split(" - ", 2);
                    return new LabTrendResponse(parts[0], parts.length > 1 ? parts[1] : "", e.getValue());
                })
                .toList();
    }

    // ──────────────────────────────────────────────
    // LAB ALERTS (ACKNOWLEDGEMENT)
    // ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<LabAlertResponse> getLabAlerts(UserPrincipal currentUser) {
        Long hospitalId = currentUser.getHospitalId();
        return alertRepository.findByStatusFetching(AlertStatus.ACTIVE).stream()
                .filter(a -> a.getHospital().getId().equals(hospitalId) && a.getLabOrder() != null)
                .map(a -> new LabAlertResponse(a.getId(), a.getPatient().getId(),
                        a.getPatient().getFirstName() + " " + a.getPatient().getLastName(),
                        a.getSeverity().name(), a.getStatus().name(), a.getMessage(),
                        a.getLabOrder().getId(),
                        a.getAcknowledgedBy() != null ? a.getAcknowledgedBy().getFullName() : null,
                        a.getCreatedAt().toLocalDateTime()))
                .toList();
    }

    @Transactional
    public LabAlertResponse acknowledgeLabAlert(Long alertId, UserPrincipal currentUser) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found"));
        tenantValidator.validateHospitalAccess(alert.getHospital().getId(), currentUser.getHospitalId());

        if (alert.getStatus() != AlertStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Alert is not active");
        }

        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedBy(currentUser.getUser());
        alert.setAcknowledgedAt(java.time.OffsetDateTime.now());
        alert = alertRepository.save(alert);

        return new LabAlertResponse(alert.getId(), alert.getPatient().getId(),
                alert.getPatient().getFirstName() + " " + alert.getPatient().getLastName(),
                alert.getSeverity().name(), alert.getStatus().name(), alert.getMessage(),
                alert.getLabOrder() != null ? alert.getLabOrder().getId() : null,
                alert.getAcknowledgedBy().getFullName(), alert.getCreatedAt().toLocalDateTime());
    }

    // ──────────────────────────────────────────────
    // DEVICE RESULT IMPORT
    // ──────────────────────────────────────────────

    @Transactional
    public DeviceImportResponse importDeviceResult(DeviceImportRequest request, UserPrincipal currentUser) {
        List<LabTestOrder> orders = labTestOrderRepository
                .findByHospitalIdAndSampleBarcode(currentUser.getHospitalId(), request.sampleId());
        if (orders.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No lab order found with sample ID: " + request.sampleId());
        }

        LabTestOrder order = orders.get(0);
        String values = request.values().toString();

        ResultEntryRequest entryReq = new ResultEntryRequest(values, "Imported from device", null);
        LabOrderResponse response = enterResults(order.getId(), entryReq, currentUser);

        return new DeviceImportResponse(response.id(), response.resultValues() != null ? 1 : 0, response.criticalFlag());
    }

    // ──────────────────────────────────────────────
    // SAMPLE LIFECYCLE
    // ──────────────────────────────────────────────

    @Transactional
    public SampleTrackingResponse disposeSample(Long trackingId, UserPrincipal currentUser) {
        SampleTracking tracking = sampleTrackingRepository.findById(trackingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sample tracking not found"));
        tenantValidator.validateHospitalAccess(tracking.getHospital().getId(), currentUser.getHospitalId());

        if (tracking.getStatus() == SampleStatus.DISPOSED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sample already disposed");
        }

        tracking.setStatus(SampleStatus.DISPOSED);
        tracking.setDisposedAt(LocalDateTime.now());
        tracking.setDisposedBy(currentUser.getUser());
        tracking = sampleTrackingRepository.save(tracking);

        return new SampleTrackingResponse(
                tracking.getId(), tracking.getLabOrder().getId(), tracking.getSampleType(),
                tracking.getContainerType(), tracking.getBarcode(), tracking.getCollectionVolume(),
                tracking.getCollectionMethod(), tracking.getStorageLocation(),
                tracking.getStorageCondition(), tracking.getCollectedBy().getFullName(),
                tracking.getCollectedAt(), tracking.getNotes());
    }

    // ──────────────────────────────────────────────
    // SLA BREACH DETECTION
    // ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SlaBreachResponse> getSlaBreaches(UserPrincipal currentUser) {
        return slaBreachRepository.findByHospitalIdOrderByBreachedAtDesc(currentUser.getHospitalId()).stream()
                .map(s -> new SlaBreachResponse(s.getId(), s.getLabOrder().getId(),
                        s.getLabOrder().getPatient().getFirstName() + " " + s.getLabOrder().getPatient().getLastName(),
                        s.getLabOrder().getTestName(), s.getExpectedTatMinutes(), s.getActualTatMinutes(),
                        s.getNotified(), s.getBreachedAt()))
                .toList();
    }

    // ──────────────────────────────────────────────
    // INTERNAL HELPERS
    // ──────────────────────────────────────────────

    private void checkSlaBreach(LocalDateTime sampleCollectedAt, String testName, LabTestOrder order, UserPrincipal currentUser) {
        if (sampleCollectedAt == null) return;
        int actualMinutes = (int) ChronoUnit.MINUTES.between(sampleCollectedAt, LocalDateTime.now());
        int expectedTat = getExpectedTat(testName);
        if (actualMinutes > expectedTat && !slaBreachRepository.existsByLabOrderId(order.getId())) {
            SlaBreach breach = SlaBreach.builder()
                    .hospital(order.getHospital())
                    .labOrder(order)
                    .expectedTatMinutes(expectedTat)
                    .actualTatMinutes(actualMinutes)
                    .build();
            slaBreachRepository.save(breach);
            eventPublisher.publishAsync(EventConstants.ALERT_CREATED, currentUser.getHospitalId(),
                    Map.of("type", "SLA_BREACH", "labOrderId", order.getId(),
                            "patientId", order.getPatient().getId(), "testName", testName));
        }
    }

    private int getExpectedTat(String testName) {
        if (testName == null) return 120;
        String upper = testName.toUpperCase();
        if (upper.contains("CBC") || upper.contains("COMPLETE BLOOD")) return 60;
        if (upper.contains("GLUCOSE") || upper.contains("BLOOD SUGAR")) return 30;
        if (upper.contains("LIPID") || upper.contains("THYROID")) return 180;
        if (upper.contains("URINE") || upper.contains("CULTURE")) return 240;
        return 120;
    }

    private void createLabAlert(LabTestOrder order, boolean isCritical, UserPrincipal currentUser) {
        if (!isCritical) return;
        Alert alert = Alert.builder()
                .hospital(order.getHospital())
                .patient(order.getPatient())
                .type(AlertType.LAB)
                .severity(AlertSeverity.CRITICAL)
                .message("Critical result for " + order.getTestName())
                .labOrder(order)
                .build();
        alertRepository.save(alert);
    }

    private boolean detectCriticalResult(LabTestOrder order, Long hospitalId) {
        List<LabCriticalRule> rules = labCriticalRuleRepository
                .findByHospitalIdAndTestNameAndEnabledTrue(hospitalId, order.getTestName());
        if (rules.isEmpty()) return false;

        String resultValues = order.getResultValues();
        if (resultValues == null || resultValues.isBlank()) return false;

        String lines = resultValues.toLowerCase();
        for (LabCriticalRule rule : rules) {
            try {
                String threshold = rule.getThresholdValue().toLowerCase();
                double thresholdVal = Double.parseDouble(threshold);
                String paramLower = rule.getParameterName().toLowerCase();

                String[] resultLines = lines.split("\n");
                for (String line : resultLines) {
                    if (line.toLowerCase().contains(paramLower)) {
                        String[] parts = line.split("[:=]");
                        if (parts.length >= 2) {
                            String valStr = parts[1].trim().split("\\s+")[0];
                            double val = Double.parseDouble(valStr);
                            boolean triggered = switch (rule.getConditionOperator()) {
                                case ">" -> val > thresholdVal;
                                case ">=" -> val >= thresholdVal;
                                case "<" -> val < thresholdVal;
                                case "<=" -> val <= thresholdVal;
                                case "==" -> val == thresholdVal;
                                case "!=" -> val != thresholdVal;
                                default -> false;
                            };
                            if (triggered) return true;
                        }
                    }
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return false;
    }

    private LabMetricsResponse computeMetrics(Long hospitalId) {
        long pendingCollection = labTestOrderRepository.countByHospitalIdAndStatus(hospitalId, LabOrderStatus.ORDERED);
        long inProcessing = labTestOrderRepository.countByHospitalIdAndStatus(hospitalId, LabOrderStatus.SAMPLE_COLLECTED)
                + labTestOrderRepository.countByHospitalIdAndStatus(hospitalId, LabOrderStatus.PROCESSING);
        long pendingApproval = labTestOrderRepository.countByHospitalIdAndStatus(hospitalId, LabOrderStatus.RESULT_ENTERED)
                + labTestOrderRepository.countByHospitalIdAndStatus(hospitalId, LabOrderStatus.PENDING_APPROVAL)
                + labTestOrderRepository.countByHospitalIdAndStatus(hospitalId, LabOrderStatus.NEEDS_RETEST);
        long completedToday = labTestOrderRepository.countByHospitalIdAndStatusUpdatedAtBetween(
                hospitalId, LabOrderStatus.APPROVED,
                LocalDateTime.now().withHour(0).withMinute(0).withSecond(0),
                LocalDateTime.now());
        long criticalResults = labTestOrderRepository.countByHospitalIdAndCriticalFlagTrue(hospitalId);
        long retests = labTestOrderRepository.findRetestsByHospitalId(hospitalId).size();
        Double avgTat = labTestOrderRepository.avgTurnaroundByHospitalId(hospitalId);
        long totalToday = labTestOrderRepository.countByHospitalIdAndCreatedAtBetween(
                hospitalId,
                LocalDateTime.now().withHour(0).withMinute(0).withSecond(0),
                LocalDateTime.now());

        return new LabMetricsResponse(pendingCollection, inProcessing, pendingApproval,
                completedToday, criticalResults, retests, avgTat, totalToday);
    }

    private LabTestOrder findOrderAndValidate(Long orderId, UserPrincipal currentUser) {
        LabTestOrder order = labTestOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab order not found"));
        tenantValidator.validateHospitalAccess(order.getHospital().getId(), currentUser.getHospitalId());
        return order;
    }

    private LabOrderListResponse mapToListResponse(LabTestOrder o) {
        return new LabOrderListResponse(
                o.getId(), o.getPatient().getId(),
                o.getPatient().getFirstName() + " " + o.getPatient().getLastName(),
                o.getTestName(), o.getRequestedBy().getFullName(),
                o.getStatus().name(), o.getCreatedAt(), o.getSampleCollectedAt(),
                o.getCriticalFlag(), o.getTurnaroundMinutes(), o.getSampleBarcode());
    }

    private LabOrderResponse mapToResponse(LabTestOrder o) {
        List<SampleTrackingResponse> trackingResponses = o.getSampleTrackings().stream()
                .map(s -> new SampleTrackingResponse(
                        s.getId(), s.getLabOrder().getId(), s.getSampleType(), s.getContainerType(),
                        s.getBarcode(), s.getCollectionVolume(), s.getCollectionMethod(),
                        s.getStorageLocation(), s.getStorageCondition(),
                        s.getCollectedBy().getFullName(), s.getCollectedAt(), s.getNotes()))
                .toList();

        return new LabOrderResponse(
                o.getId(), o.getPatient().getId(),
                o.getPatient().getFirstName() + " " + o.getPatient().getLastName(),
                o.getTestName(), o.getRequestedBy().getFullName(),
                o.getStatus().name(), o.getResultValues(), o.getTechnicianNotes(),
                o.getCompletedAt(), o.getCreatedAt(), o.getSampleCollectedAt(),
                o.getProcessingStartedAt(), o.getProcessingCompletedAt(),
                o.getSampleBarcode(), o.getSampleStorageLocation(),
                o.getCriticalFlag(), o.getTurnaroundMinutes(),
                o.getRetestOf() != null ? o.getRetestOf().getId() : null,
                o.getCorrectionReason(),
                o.getResultEnteredBy() != null ? o.getResultEnteredBy().getFullName() : null,
                o.getPrice(), trackingResponses);
    }
}
