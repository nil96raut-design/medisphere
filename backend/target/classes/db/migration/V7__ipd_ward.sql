CREATE TABLE bed (
    id BIGSERIAL PRIMARY KEY,
    hospital_id BIGINT NOT NULL REFERENCES hospital(id),
    ward_name VARCHAR(255) NOT NULL,
    bed_number VARCHAR(50) NOT NULL,
    charge_per_day DECIMAL(10, 2) NOT NULL DEFAULT 0,
    is_occupied BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX idx_bed_hospital_ward_number ON bed(hospital_id, ward_name, bed_number);
CREATE INDEX idx_bed_hospital ON bed(hospital_id);

CREATE TABLE admission (
    id BIGSERIAL PRIMARY KEY,
    hospital_id BIGINT NOT NULL REFERENCES hospital(id),
    patient_id BIGINT NOT NULL REFERENCES patient(id),
    doctor_id BIGINT NOT NULL REFERENCES app_user(id),
    bed_id BIGINT REFERENCES bed(id),
    admission_date DATE NOT NULL,
    discharge_date DATE,
    initial_diagnosis TEXT,
    discharge_summary TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'ADMITTED'
);

CREATE INDEX idx_admission_hospital ON admission(hospital_id);
CREATE INDEX idx_admission_patient ON admission(patient_id);
CREATE INDEX idx_admission_bed ON admission(bed_id);

CREATE TABLE nursing_log (
    id BIGSERIAL PRIMARY KEY,
    admission_id BIGINT NOT NULL REFERENCES admission(id),
    nurse_id BIGINT NOT NULL REFERENCES app_user(id),
    vitals_recorded TEXT,
    medicine_administered TEXT,
    nursing_notes TEXT,
    logged_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_nursing_log_admission ON nursing_log(admission_id);
