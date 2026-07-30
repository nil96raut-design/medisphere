-- ============================================================
-- V28: Nurse Module — assignments, vitals, MAR, notes, tasks
-- ============================================================

CREATE TABLE IF NOT EXISTS nurse_assignment (
    id              BIGSERIAL PRIMARY KEY,
    hospital_id     BIGINT NOT NULL REFERENCES hospital(id),
    nurse_id        BIGINT NOT NULL REFERENCES app_user(id),
    patient_id      BIGINT NOT NULL REFERENCES patient(id),
    bed_id          BIGINT REFERENCES bed(id),
    ward_name       VARCHAR(100),
    assigned_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    released_at     TIMESTAMP,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT uq_nurse_assignment_active UNIQUE (patient_id)
);

CREATE INDEX IF NOT EXISTS idx_na_nurse ON nurse_assignment(nurse_id);
CREATE INDEX IF NOT EXISTS idx_na_status ON nurse_assignment(status);
CREATE INDEX IF NOT EXISTS idx_na_hospital ON nurse_assignment(hospital_id);

CREATE TABLE IF NOT EXISTS vital_record (
    id              BIGSERIAL PRIMARY KEY,
    hospital_id     BIGINT NOT NULL REFERENCES hospital(id),
    patient_id      BIGINT NOT NULL REFERENCES patient(id),
    nurse_id        BIGINT NOT NULL REFERENCES app_user(id),
    blood_pressure  VARCHAR(20),
    heart_rate      INTEGER,
    temperature     DECIMAL(4,1),
    spo2            INTEGER,
    sugar_level     DECIMAL(5,2),
    alert_flag      BOOLEAN NOT NULL DEFAULT FALSE,
    alert_reason    VARCHAR(255),
    recorded_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_vr_patient ON vital_record(patient_id);
CREATE INDEX IF NOT EXISTS idx_vr_recorded ON vital_record(recorded_at);
CREATE INDEX IF NOT EXISTS idx_vr_hospital ON vital_record(hospital_id);

CREATE TABLE IF NOT EXISTS medication_administration (
    id                  BIGSERIAL PRIMARY KEY,
    hospital_id         BIGINT NOT NULL REFERENCES hospital(id),
    prescription_item_id BIGINT NOT NULL REFERENCES prescription_item(id),
    patient_id          BIGINT NOT NULL REFERENCES patient(id),
    nurse_id            BIGINT NOT NULL REFERENCES app_user(id),
    status              VARCHAR(20) NOT NULL DEFAULT 'GIVEN',
    administered_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    notes               TEXT,
    CONSTRAINT uq_mar_scheduled UNIQUE (prescription_item_id)
);

CREATE INDEX IF NOT EXISTS idx_ma_patient ON medication_administration(patient_id);
CREATE INDEX IF NOT EXISTS idx_ma_nurse ON medication_administration(nurse_id);
CREATE INDEX IF NOT EXISTS idx_ma_hospital ON medication_administration(hospital_id);

CREATE TABLE IF NOT EXISTS nursing_note (
    id          BIGSERIAL PRIMARY KEY,
    hospital_id BIGINT NOT NULL REFERENCES hospital(id),
    patient_id  BIGINT NOT NULL REFERENCES patient(id),
    nurse_id    BIGINT NOT NULL REFERENCES app_user(id),
    note        TEXT NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_nn_patient ON nursing_note(patient_id);
CREATE INDEX IF NOT EXISTS idx_nn_nurse ON nursing_note(nurse_id);
CREATE INDEX IF NOT EXISTS idx_nn_hospital ON nursing_note(hospital_id);

CREATE TABLE IF NOT EXISTS nurse_task (
    id          BIGSERIAL PRIMARY KEY,
    hospital_id BIGINT NOT NULL REFERENCES hospital(id),
    nurse_id    BIGINT NOT NULL REFERENCES app_user(id),
    patient_id  BIGINT NOT NULL REFERENCES patient(id),
    task_type   VARCHAR(30) NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    due_time    TIMESTAMP,
    completed_at TIMESTAMP,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_nt_nurse ON nurse_task(nurse_id);
CREATE INDEX IF NOT EXISTS idx_nt_patient ON nurse_task(patient_id);
CREATE INDEX IF NOT EXISTS idx_nt_status ON nurse_task(status);
CREATE INDEX IF NOT EXISTS idx_nt_hospital ON nurse_task(hospital_id);
