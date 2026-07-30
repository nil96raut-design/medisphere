CREATE TABLE walk_in_queue (
    id              BIGSERIAL PRIMARY KEY,
    hospital_id     BIGINT NOT NULL REFERENCES hospital(id),
    patient_id      BIGINT NOT NULL REFERENCES patient(id),
    doctor_id       BIGINT NOT NULL REFERENCES doctor(id),
    created_by      BIGINT NOT NULL REFERENCES app_user(id),
    token_no        INTEGER NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    priority        VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    notes           VARCHAR(500),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_walkin_token UNIQUE (hospital_id, doctor_id, token_no)
);

CREATE INDEX idx_walkin_doctor_date ON walk_in_queue(doctor_id, created_at);
CREATE INDEX idx_walkin_doctor_status ON walk_in_queue(doctor_id, status);
CREATE INDEX idx_walkin_hospital ON walk_in_queue(hospital_id);
