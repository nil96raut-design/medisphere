-- PERFORMANCE & SCALING INDEXES
CREATE INDEX IF NOT EXISTS idx_appointment_patient_date ON appointment(patient_id, appointment_date);
CREATE INDEX IF NOT EXISTS idx_vital_record_patient_time ON vital_record(patient_id, recorded_at DESC);
CREATE INDEX IF NOT EXISTS idx_lab_order_patient_status ON lab_test_order(patient_id, status);
CREATE INDEX IF NOT EXISTS idx_bill_patient_status ON bill(patient_id, payment_status);
CREATE INDEX IF NOT EXISTS idx_dispensation_patient_billing ON dispensation_record(patient_id, billing_status);
