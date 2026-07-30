-- ============================================================
-- V20: Soft delete support for app_user
-- ============================================================
-- FULLY IDEMPOTENT.

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'app_user' AND column_name = 'deleted') THEN
        ALTER TABLE app_user ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'app_user' AND column_name = 'deleted_at') THEN
        ALTER TABLE app_user ADD COLUMN deleted_at TIMESTAMPTZ;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'patient' AND column_name = 'deleted') THEN
        ALTER TABLE patient ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'patient' AND column_name = 'deleted_at') THEN
        ALTER TABLE patient ADD COLUMN deleted_at TIMESTAMPTZ;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_app_user_deleted ON app_user(deleted);
CREATE INDEX IF NOT EXISTS idx_patient_deleted ON patient(deleted);
