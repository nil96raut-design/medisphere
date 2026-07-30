-- ============================================================
-- V17: Track billed items (bill_id FK on domain entities)
-- ============================================================
-- FULLY IDEMPOTENT.

-- 1. Add bill_id to appointment
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'appointment' AND column_name = 'bill_id') THEN
        ALTER TABLE appointment ADD COLUMN bill_id BIGINT REFERENCES bill(id);
    END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_appointment_bill ON appointment(bill_id);

-- 2. Add bill_id to lab_test_order
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'lab_test_order' AND column_name = 'bill_id') THEN
        ALTER TABLE lab_test_order ADD COLUMN bill_id BIGINT REFERENCES bill(id);
    END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_lab_bill ON lab_test_order(bill_id);

-- 3. Add bill_id to dispensation_record
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'dispensation_record' AND column_name = 'bill_id') THEN
        ALTER TABLE dispensation_record ADD COLUMN bill_id BIGINT REFERENCES bill(id);
    END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_dispensation_bill ON dispensation_record(bill_id);

-- 4. Add UNIQUE constraint on (patient_id, idempotency_key) for stronger idempotency
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_bill_patient_idempotency') THEN
        ALTER TABLE bill ADD CONSTRAINT uq_bill_patient_idempotency UNIQUE (patient_id, idempotency_key);
    END IF;
END $$;
