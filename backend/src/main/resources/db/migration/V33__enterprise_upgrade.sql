-- 1. LAB RESULT VERSIONING
CREATE TABLE IF NOT EXISTS lab_result_history (
    id BIGSERIAL PRIMARY KEY,
    lab_order_id BIGINT NOT NULL REFERENCES lab_test_order(id),
    version INT NOT NULL,
    result_data TEXT,
    created_by BIGINT NOT NULL REFERENCES app_user(id),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(lab_order_id, version)
);

-- 2. ALERT - add lab_order_id reference (nullable, backward compatible)
ALTER TABLE alert ADD COLUMN IF NOT EXISTS lab_order_id BIGINT REFERENCES lab_test_order(id);

-- 3. SAMPLE LIFECYCLE STATUS
ALTER TABLE sample_tracking ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'COLLECTED';
ALTER TABLE sample_tracking ADD COLUMN IF NOT EXISTS disposed_at TIMESTAMP;
ALTER TABLE sample_tracking ADD COLUMN IF NOT EXISTS disposed_by BIGINT REFERENCES app_user(id);
ALTER TABLE sample_tracking ADD COLUMN IF NOT EXISTS retention_days INT DEFAULT 30;

-- 4. SLA BREACH TABLE
CREATE TABLE IF NOT EXISTS sla_breach (
    id BIGSERIAL PRIMARY KEY,
    hospital_id BIGINT NOT NULL REFERENCES hospital(id),
    lab_order_id BIGINT NOT NULL REFERENCES lab_test_order(id),
    expected_tat_minutes INT NOT NULL,
    actual_tat_minutes INT NOT NULL,
    breached_at TIMESTAMP NOT NULL DEFAULT NOW(),
    notified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 5. MEDICATION SCHEDULE - add hospital_id for tenant isolation
ALTER TABLE medication_schedule ADD COLUMN IF NOT EXISTS hospital_id BIGINT REFERENCES hospital(id);
ALTER TABLE medication_schedule ADD COLUMN IF NOT EXISTS tenant_isolation BOOLEAN NOT NULL DEFAULT FALSE;

-- 5b. HOSPITAL INVITATION CODE
ALTER TABLE hospital ADD COLUMN IF NOT EXISTS invitation_code VARCHAR(50) UNIQUE;

-- 6. INDEXES
CREATE INDEX IF NOT EXISTS idx_lab_result_history_order ON lab_result_history(lab_order_id, version);
CREATE INDEX IF NOT EXISTS idx_lab_result_history_active ON lab_result_history(lab_order_id) WHERE is_active = TRUE;
CREATE INDEX IF NOT EXISTS idx_alert_lab_order ON alert(lab_order_id);
CREATE INDEX IF NOT EXISTS idx_sla_breach_hospital ON sla_breach(hospital_id);
CREATE INDEX IF NOT EXISTS idx_sla_breach_notified ON sla_breach(notified) WHERE notified = FALSE;
CREATE INDEX IF NOT EXISTS idx_sample_tracking_status ON sample_tracking(hospital_id, status);
