-- ============================================================
-- V31: Audit, index, and data integrity fixes
-- ============================================================
-- FULLY IDEMPOTENT.

-- 1. Indexes for audit_log performance
CREATE INDEX IF NOT EXISTS idx_audit_log_hospital ON audit_log(hospital_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_action ON audit_log(action);
CREATE INDEX IF NOT EXISTS idx_audit_log_user ON audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_timestamp ON audit_log(timestamp);

-- 2. Index for bill refund queries
CREATE INDEX IF NOT EXISTS idx_bill_patient ON bill(patient_id);
CREATE INDEX IF NOT EXISTS idx_bill_payment_status ON bill(payment_status);
CREATE INDEX IF NOT EXISTS idx_bill_created_at ON bill(created_at);

-- 3. Index for billed_item lookups
CREATE INDEX IF NOT EXISTS idx_billed_item_bill ON billed_item(bill_id);
CREATE INDEX IF NOT EXISTS idx_billed_item_source ON billed_item(source_type, source_id);

-- 4. Index for insurance claims
CREATE INDEX IF NOT EXISTS idx_insurance_claim_bill ON insurance_claim(bill_id);
CREATE INDEX IF NOT EXISTS idx_insurance_claim_status ON insurance_claim(status);

-- 5. Index for notification queries (common in dashboards)
CREATE INDEX IF NOT EXISTS idx_notification_user ON notification(user_id);
CREATE INDEX IF NOT EXISTS idx_notification_status ON notification(status);

-- 6. Index for NurseTask queries (common in shift handover)
CREATE INDEX IF NOT EXISTS idx_nurse_task_nurse ON nurse_task(nurse_id);
CREATE INDEX IF NOT EXISTS idx_nurse_task_status ON nurse_task(status);

-- 7. Index for WalkInQueue performance
CREATE INDEX IF NOT EXISTS idx_walk_in_queue_doctor ON walk_in_queue(doctor_id);
CREATE INDEX IF NOT EXISTS idx_walk_in_queue_status ON walk_in_queue(status);
