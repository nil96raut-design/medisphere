CREATE TABLE doctor (
    id BIGSERIAL PRIMARY KEY,
    hospital_id BIGINT NOT NULL REFERENCES hospital(id),
    user_id BIGINT NOT NULL REFERENCES app_user(id),
    specialization VARCHAR(255) NOT NULL,
    consultation_fee DECIMAL(10, 2),
    is_available BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_doctor_hospital ON doctor(hospital_id);
CREATE INDEX idx_doctor_user ON doctor(user_id);

CREATE TABLE appointment (
    id BIGSERIAL PRIMARY KEY,
    hospital_id BIGINT NOT NULL REFERENCES hospital(id),
    patient_id BIGINT NOT NULL REFERENCES patient(id),
    doctor_id BIGINT NOT NULL REFERENCES doctor(id),
    appointment_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status VARCHAR(32) NOT NULL,
    token_number INTEGER
);

CREATE INDEX idx_appointment_hospital ON appointment(hospital_id);
CREATE INDEX idx_appointment_patient ON appointment(patient_id);
CREATE INDEX idx_appointment_doctor_date ON appointment(doctor_id, appointment_date);
