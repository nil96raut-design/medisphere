-- ============================================================
-- ENTERPRISE SCALING & PERFORMANCE HARDENING
-- Indexes for analytics, pagination, notification throughput
-- ============================================================

-- Analytics: revenue aggregation index
CREATE INDEX IF NOT EXISTS idx_bill_hospital_status_created
    ON bill (hospital_id, status, created_at)
    WHERE status = 'PAID';

-- Analytics: lab volume by hospital
CREATE INDEX IF NOT EXISTS idx_lab_order_hospital_status
    ON lab_test_order (hospital_id, status);

-- Analytics: lab volume monthly trend
CREATE INDEX IF NOT EXISTS idx_lab_order_hospital_created
    ON lab_test_order (hospital_id, created_at);

-- Analytics: pharmacy dispensation by hospital
CREATE INDEX IF NOT EXISTS idx_dispensation_hospital_created
    ON dispensation_record (hospital_id, dispensed_at);

-- Notification pagination
CREATE INDEX IF NOT EXISTS idx_notification_user_created
    ON notification (user_id, created_at DESC);

-- Background job performance
CREATE INDEX IF NOT EXISTS idx_background_job_status_next_attempt
    ON background_job (status, next_attempt_at)
    WHERE status = 'PENDING';

-- Billing: patient bill pagination
CREATE INDEX IF NOT EXISTS idx_bill_patient_created
    ON bill (patient_id, created_at DESC);

-- Appointment: queue queries
CREATE INDEX IF NOT EXISTS idx_appointment_doctor_date_status
    ON appointment (doctor_id, appointment_date, status);

-- Audit log: filtered search
CREATE INDEX IF NOT EXISTS idx_audit_log_hospital_action
    ON audit_log (hospital_id, action, timestamp DESC);

-- Medicine stock: low stock alerts
CREATE INDEX IF NOT EXISTS idx_medicine_stock_hospital_level
    ON medicine_stock (hospital_id, available_quantity, reorder_level);

-- Billed item: double-billing prevention fast lookup
CREATE UNIQUE INDEX IF NOT EXISTS idx_billed_item_source
    ON billed_item (source_type, source_id);

-- Notification: status-based queries
CREATE INDEX IF NOT EXISTS idx_notification_status
    ON notification (status, user_id);

-- ============================================================
-- TABLE MAINTENANCE CONFIGURATION
-- Enable autovacuum tuning for high-traffic tables
-- ============================================================

ALTER TABLE IF EXISTS bill SET (
    autovacuum_vacuum_scale_factor = 0.01,
    autovacuum_analyze_scale_factor = 0.005
);

ALTER TABLE IF EXISTS notification SET (
    autovacuum_vacuum_scale_factor = 0.01,
    autovacuum_analyze_scale_factor = 0.005
);

ALTER TABLE IF EXISTS background_job SET (
    autovacuum_vacuum_scale_factor = 0.01,
    autovacuum_analyze_scale_factor = 0.005
);
