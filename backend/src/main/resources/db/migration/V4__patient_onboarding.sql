CREATE TABLE patient (
    id BIGSERIAL PRIMARY KEY,
    hospital_id BIGINT NOT NULL REFERENCES hospital(id),
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    gender VARCHAR(50),
    date_of_birth DATE,
    phone_number VARCHAR(50) NOT NULL,
    email VARCHAR(255),
    emergency_contact VARCHAR(255),
    insurance_provider VARCHAR(255),
    policy_number VARCHAR(255)
);

CREATE UNIQUE INDEX idx_patient_hospital_phone ON patient(hospital_id, phone_number);

CREATE TABLE triage (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patient(id),
    hospital_id BIGINT NOT NULL REFERENCES hospital(id),
    blood_pressure VARCHAR(50),
    temperature_celsius DOUBLE PRECISION,
    pulse_rate INTEGER,
    weight_kg DOUBLE PRECISION,
    recorded_at TIMESTAMP NOT NULL,
    recorded_by BIGINT NOT NULL REFERENCES app_user(id)
);
