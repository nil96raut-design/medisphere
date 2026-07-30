package com.healthtrack.event;

public final class EventConstants {

    private EventConstants() {}

    public static final String APPOINTMENT_CREATED = "APPOINTMENT_CREATED";
    public static final String BILL_GENERATED = "BILL_GENERATED";
    public static final String LAB_RESULT_READY = "LAB_RESULT_READY";
    public static final String PRESCRIPTION_DISPENSED = "PRESCRIPTION_DISPENSED";
    public static final String PATIENT_ADMITTED = "PATIENT_ADMITTED";
    public static final String PATIENT_DISCHARGED = "PATIENT_DISCHARGED";
    public static final String INSURANCE_CLAIM_SUBMITTED = "INSURANCE_CLAIM_SUBMITTED";
    public static final String WALK_IN_ADDED = "WALK_IN_ADDED";

    public static final String VITALS_RECORDED = "VITALS_RECORDED";
    public static final String CRITICAL_VITAL = "CRITICAL_VITAL";
    public static final String MEDICATION_GIVEN = "MEDICATION_GIVEN";
    public static final String SAMPLE_COLLECTED = "SAMPLE_COLLECTED";
    public static final String NURSE_NOTE_ADDED = "NURSE_NOTE_ADDED";
    public static final String NURSE_ASSIGNED = "NURSE_ASSIGNED";
    public static final String NURSE_TASK_CREATED = "NURSE_TASK_CREATED";
    public static final String NURSE_TASK_COMPLETED = "NURSE_TASK_COMPLETED";

    public static final String MEDICATION_SCHEDULED = "MEDICATION_SCHEDULED";
    public static final String MEDICATION_MISSED = "MEDICATION_MISSED";
    public static final String CRITICAL_VITAL_ESCALATED = "CRITICAL_VITAL_ESCALATED";
    public static final String ALERT_CREATED = "ALERT_CREATED";
    public static final String ALERT_ESCALATED = "ALERT_ESCALATED";
    public static final String ALERT_ACKNOWLEDGED = "ALERT_ACKNOWLEDGED";
    public static final String ALERT_RESOLVED = "ALERT_RESOLVED";
    public static final String SHIFT_HANDOVER_COMPLETED = "SHIFT_HANDOVER_COMPLETED";
    public static final String BED_CLEANING_REQUESTED = "BED_CLEANING_REQUESTED";
    public static final String BED_CLEANED = "BED_CLEANED";
    public static final String RECURRING_TASK_CREATED = "RECURRING_TASK_CREATED";

    public static final String DISPENSE_MEDICINE = "DISPENSE_MEDICINE";
    public static final String PARTIAL_DISPENSE = "PARTIAL_DISPENSE";
    public static final String EXPIRED_BLOCKED = "EXPIRED_BLOCKED";
    public static final String REORDER_CREATED = "REORDER_CREATED";
    public static final String EXPIRY_ALERT = "EXPIRY_ALERT";
    public static final String NEAR_EXPIRY_ALERT = "NEAR_EXPIRY_ALERT";

    public static final String LAB_PROCESSING = "LAB_PROCESSING";
    public static final String LAB_RESULT_ENTERED = "LAB_RESULT_ENTERED";
    public static final String CRITICAL_LAB_RESULT = "CRITICAL_LAB_RESULT";
    public static final String LAB_RETEST_REQUESTED = "LAB_RETEST_REQUESTED";
}
