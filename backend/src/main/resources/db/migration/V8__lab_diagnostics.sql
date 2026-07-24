CREATE TABLE lab_test_order (
    id BIGSERIAL PRIMARY KEY,
    hospital_id BIGINT NOT NULL REFERENCES hospital(id),
    patient_id BIGINT NOT NULL REFERENCES patient(id),
    medical_record_id BIGINT REFERENCES medical_record(id),
    test_name VARCHAR(255) NOT NULL,
    requested_by BIGINT NOT NULL REFERENCES app_user(id),
    status VARCHAR(50) NOT NULL DEFAULT 'ORDERED',
    result_values TEXT,
    technician_notes TEXT,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_lab_order_hospital ON lab_test_order(hospital_id);
CREATE INDEX idx_lab_order_patient ON lab_test_order(patient_id);
CREATE INDEX idx_lab_order_status ON lab_test_order(status);
