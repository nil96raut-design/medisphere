-- ============================================================
-- V18: Appointment NO_SHOW + lab approval workflow
-- ============================================================
-- FULLY IDEMPOTENT.

-- 1. Add approved_by / approved_at to lab_test_order
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'lab_test_order' AND column_name = 'approved_by') THEN
        ALTER TABLE lab_test_order ADD COLUMN approved_by BIGINT REFERENCES app_user(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'lab_test_order' AND column_name = 'approved_at') THEN
        ALTER TABLE lab_test_order ADD COLUMN approved_at TIMESTAMPTZ;
    END IF;
END $$;

-- 2. Add new lab status PENDING_APPROVAL if missing (Java enum handles the rest)
--    The status is stored as VARCHAR, so no schema change needed for enum values.

-- 3. Index for lab orders by patient for the new patient portal
CREATE INDEX IF NOT EXISTS idx_lab_patient_status ON lab_test_order(patient_id, status);
