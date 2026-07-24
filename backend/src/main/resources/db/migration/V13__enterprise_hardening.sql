-- ============================================================
-- V13: Enterprise hardening — idempotency, timezone, audit log
-- ============================================================

-- 1. Idempotency key on bill
ALTER TABLE bill ADD COLUMN idempotency_key VARCHAR(255);
UPDATE bill SET idempotency_key = 'migration-' || id WHERE idempotency_key IS NULL;
ALTER TABLE bill ALTER COLUMN idempotency_key SET NOT NULL;
ALTER TABLE bill ADD CONSTRAINT uq_bill_idempotency_key UNIQUE (idempotency_key);
CREATE INDEX idx_bill_idempotency_key ON bill(idempotency_key);

-- 2. Convert timestamp columns to TIMESTAMPTZ (UTC)
ALTER TABLE bill ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';
ALTER TABLE hospital ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';
ALTER TABLE hospital ALTER COLUMN trial_end_date TYPE TIMESTAMPTZ USING trial_end_date AT TIME ZONE 'UTC';
ALTER TABLE subscription ALTER COLUMN start_date TYPE TIMESTAMPTZ USING start_date AT TIME ZONE 'UTC';
ALTER TABLE subscription ALTER COLUMN end_date TYPE TIMESTAMPTZ USING end_date AT TIME ZONE 'UTC';

-- 3. Audit log table
CREATE TABLE audit_log (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    hospital_id     BIGINT NOT NULL,
    action          VARCHAR(100) NOT NULL,
    entity          VARCHAR(100) NOT NULL,
    entity_id       BIGINT,
    details         TEXT,
    timestamp       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_log_hospital ON audit_log(hospital_id);
CREATE INDEX idx_audit_log_user ON audit_log(user_id);
CREATE INDEX idx_audit_log_action ON audit_log(action);
