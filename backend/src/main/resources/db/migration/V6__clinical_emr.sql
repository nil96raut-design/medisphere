CREATE TABLE medical_record (
    id BIGSERIAL PRIMARY KEY,
    hospital_id BIGINT NOT NULL REFERENCES hospital(id),
    patient_id BIGINT NOT NULL REFERENCES patient(id),
    doctor_id BIGINT NOT NULL REFERENCES app_user(id),
    appointment_id BIGINT REFERENCES appointment(id),
    encounter_date DATE NOT NULL,
    chief_complaints TEXT,
    objective_findings TEXT,
    diagnosis TEXT,
    next_follow_up_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_medical_record_hospital ON medical_record(hospital_id);
CREATE INDEX idx_medical_record_patient ON medical_record(patient_id);
CREATE INDEX idx_medical_record_doctor ON medical_record(doctor_id);

CREATE TABLE prescription_item (
    id BIGSERIAL PRIMARY KEY,
    medical_record_id BIGINT NOT NULL REFERENCES medical_record(id),
    medicine_name VARCHAR(255) NOT NULL,
    dosage VARCHAR(255) NOT NULL,
    frequency VARCHAR(255) NOT NULL,
    duration VARCHAR(255) NOT NULL,
    instructions TEXT
);

CREATE INDEX idx_prescription_record ON prescription_item(medical_record_id);

CREATE TABLE service_request (
    id BIGSERIAL PRIMARY KEY,
    medical_record_id BIGINT NOT NULL REFERENCES medical_record(id),
    hospital_id BIGINT NOT NULL REFERENCES hospital(id),
    service_type VARCHAR(50) NOT NULL,
    service_details TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING'
);

CREATE INDEX idx_service_request_record ON service_request(medical_record_id);
CREATE INDEX idx_service_request_hospital ON service_request(hospital_id);
