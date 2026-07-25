-- ============================================================
-- V13: Enterprise hardening — idempotency, timezone, audit log
-- ============================================================
-- FULLY IDEMPOTENT. Safe on fresh DB, partial migration, re-deploy.

-- ___________________________________________________________________
-- 1. Idempotency key on bill
-- ___________________________________________________________________

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'bill' AND column_name = 'idempotency_key'
    ) THEN
        ALTER TABLE bill ADD COLUMN idempotency_key VARCHAR(255);
    END IF;
END $$;

UPDATE bill SET idempotency_key = 'migration-' || id WHERE idempotency_key IS NULL;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'bill' AND column_name = 'idempotency_key'
          AND is_nullable = 'YES'
    ) THEN
        ALTER TABLE bill ALTER COLUMN idempotency_key SET NOT NULL;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_bill_idempotency_key') THEN
        ALTER TABLE bill ADD CONSTRAINT uq_bill_idempotency_key UNIQUE (idempotency_key);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_bill_idempotency_key ON bill(idempotency_key);

-- ___________________________________________________________________
-- 2. Convert timestamp columns to TIMESTAMPTZ (UTC)
--    Only convert columns that are still TIMESTAMP (not already TIMESTAMPTZ)
--    Prevents data corruption on re-run (converting TIMESTAMPTZ twice shifts times)
-- ===================================================================

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'bill' AND column_name = 'created_at'
          AND data_type = 'timestamp without time zone'
    ) THEN
        ALTER TABLE bill ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'hospital' AND column_name = 'created_at'
          AND data_type = 'timestamp without time zone'
    ) THEN
        ALTER TABLE hospital ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'hospital' AND column_name = 'trial_end_date'
          AND data_type = 'timestamp without time zone'
    ) THEN
        ALTER TABLE hospital ALTER COLUMN trial_end_date TYPE TIMESTAMPTZ USING trial_end_date AT TIME ZONE 'UTC';
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'subscription' AND column_name = 'start_date'
          AND data_type = 'timestamp without time zone'
    ) THEN
        ALTER TABLE subscription ALTER COLUMN start_date TYPE TIMESTAMPTZ USING start_date AT TIME ZONE 'UTC';
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'subscription' AND column_name = 'end_date'
          AND data_type = 'timestamp without time zone'
    ) THEN
        ALTER TABLE subscription ALTER COLUMN end_date TYPE TIMESTAMPTZ USING end_date AT TIME ZONE 'UTC';
    END IF;
END $$;

-- ___________________________________________________________________
-- 3. Audit log table
-- ___________________________________________________________________

CREATE TABLE IF NOT EXISTS audit_log (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    hospital_id     BIGINT NOT NULL,
    action          VARCHAR(100) NOT NULL,
    entity          VARCHAR(100) NOT NULL,
    entity_id       BIGINT,
    details         TEXT,
    timestamp       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_log_hospital ON audit_log(hospital_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_user ON audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_action ON audit_log(action);
