-- ============================================================
-- V29: Clinical Safety — Medication Schedule, Alerts, Shift Handover, Bed Cleaning
-- ============================================================

CREATE TABLE IF NOT EXISTS medication_schedule (
    id                   BIGSERIAL PRIMARY KEY,
    prescription_item_id BIGINT NOT NULL REFERENCES prescription_item(id),
    patient_id           BIGINT NOT NULL REFERENCES patient(id),
    nurse_id             BIGINT REFERENCES app_user(id),
    scheduled_time       TIMESTAMP NOT NULL,
    status               VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at           TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_med_schedule UNIQUE (prescription_item_id, scheduled_time)
);

CREATE INDEX IF NOT EXISTS idx_ms_patient ON medication_schedule(patient_id);
CREATE INDEX IF NOT EXISTS idx_ms_status ON medication_schedule(status);
CREATE INDEX IF NOT EXISTS idx_ms_scheduled ON medication_schedule(scheduled_time);

CREATE TABLE IF NOT EXISTS alert (
    id               BIGSERIAL PRIMARY KEY,
    hospital_id      BIGINT NOT NULL REFERENCES hospital(id),
    patient_id       BIGINT NOT NULL REFERENCES patient(id),
    type             VARCHAR(30) NOT NULL,
    severity         VARCHAR(20) NOT NULL DEFAULT 'HIGH',
    status           VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    message          TEXT,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    acknowledged_by  BIGINT REFERENCES app_user(id),
    acknowledged_at  TIMESTAMP,
    escalated_to     BIGINT REFERENCES app_user(id),
    escalated_at     TIMESTAMP,
    resolved_by      BIGINT REFERENCES app_user(id),
    resolved_at      TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_alert_patient ON alert(patient_id);
CREATE INDEX IF NOT EXISTS idx_alert_status ON alert(status);
CREATE INDEX IF NOT EXISTS idx_alert_hospital ON alert(hospital_id);

CREATE TABLE IF NOT EXISTS shift_handover (
    id            BIGSERIAL PRIMARY KEY,
    hospital_id   BIGINT NOT NULL REFERENCES hospital(id),
    from_nurse_id BIGINT NOT NULL REFERENCES app_user(id),
    to_nurse_id   BIGINT NOT NULL REFERENCES app_user(id),
    ward_name     VARCHAR(100),
    notes         TEXT,
    patient_summary TEXT,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sh_hospital ON shift_handover(hospital_id);
CREATE INDEX IF NOT EXISTS idx_sh_from ON shift_handover(from_nurse_id);
CREATE INDEX IF NOT EXISTS idx_sh_to ON shift_handover(to_nurse_id);

CREATE TABLE IF NOT EXISTS bed_cleaning_request (
    id            BIGSERIAL PRIMARY KEY,
    hospital_id   BIGINT NOT NULL REFERENCES hospital(id),
    bed_id        BIGINT NOT NULL REFERENCES bed(id),
    requested_by  BIGINT NOT NULL REFERENCES app_user(id),
    cleaned_by    BIGINT REFERENCES app_user(id),
    status        VARCHAR(20) NOT NULL DEFAULT 'REQUESTED',
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    cleaned_at    TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_bcr_bed ON bed_cleaning_request(bed_id);
CREATE INDEX IF NOT EXISTS idx_bcr_status ON bed_cleaning_request(status);
CREATE INDEX IF NOT EXISTS idx_bcr_hospital ON bed_cleaning_request(hospital_id);

ALTER TABLE nurse_task ADD COLUMN IF NOT EXISTS is_recurring BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE nurse_task ADD COLUMN IF NOT EXISTS recurrence_interval_minutes INTEGER;
ALTER TABLE nurse_task ADD COLUMN IF NOT EXISTS priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL';
ALTER TABLE nurse_task ADD COLUMN IF NOT EXISTS source VARCHAR(30);

CREATE TABLE IF NOT EXISTS idempotency_record (
    id           BIGSERIAL PRIMARY KEY,
    request_id   VARCHAR(100) NOT NULL,
    hospital_id  BIGINT NOT NULL REFERENCES hospital(id),
    action_type  VARCHAR(50) NOT NULL,
    result       TEXT,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_request_id UNIQUE (request_id)
);

CREATE INDEX IF NOT EXISTS idx_ir_request ON idempotency_record(request_id);
CREATE INDEX IF NOT EXISTS idx_ir_hospital ON idempotency_record(hospital_id);
