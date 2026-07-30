package com.healthtrack.entity;

public enum LabOrderStatus {
    ORDERED,
    SAMPLE_COLLECTED,
    PROCESSING,
    RESULT_ENTERED,
    PENDING_APPROVAL,
    APPROVED,
    NEEDS_RETEST,
    CANCELLED
}
