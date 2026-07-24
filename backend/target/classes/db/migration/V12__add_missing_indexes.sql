-- ============================================================
-- V12: Add missing FK indexes for query performance
-- ============================================================

CREATE INDEX IF NOT EXISTS idx_billing_transaction_hospital   ON billing_transaction(hospital_id);
CREATE INDEX IF NOT EXISTS idx_billing_transaction_patient    ON billing_transaction(patient_id);
CREATE INDEX IF NOT EXISTS idx_bill_patient                   ON bill(patient_id);
CREATE INDEX IF NOT EXISTS idx_dispensation_patient           ON dispensation_record(patient_id);
CREATE INDEX IF NOT EXISTS idx_dispensation_billing_status    ON dispensation_record(billing_status);
CREATE INDEX IF NOT EXISTS idx_nursing_log_admission          ON nursing_log(admission_id);
CREATE INDEX IF NOT EXISTS idx_progress_note_task             ON progress_note(task_id);
CREATE INDEX IF NOT EXISTS idx_task_assignee                  ON task(assignee_id);
CREATE INDEX IF NOT EXISTS idx_task_status                    ON task(status);
CREATE INDEX IF NOT EXISTS idx_admission_patient              ON admission(patient_id);
CREATE INDEX IF NOT EXISTS idx_admission_status               ON admission(status);
CREATE INDEX IF NOT EXISTS idx_medical_record_patient         ON medical_record(patient_id);
CREATE INDEX IF NOT EXISTS idx_appointment_patient            ON appointment(patient_id);
CREATE INDEX IF NOT EXISTS idx_lab_test_order_patient         ON lab_test_order(patient_id);
