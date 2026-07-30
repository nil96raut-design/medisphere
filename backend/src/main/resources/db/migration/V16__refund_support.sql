-- ============================================================
-- V16: Refund / void support + bill status tracking
-- ============================================================
-- FULLY IDEMPOTENT. Safe on fresh DB, partial migration, re-deploy.

-- 1. Add refund columns to bill
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'bill' AND column_name = 'status') THEN
        ALTER TABLE bill ADD COLUMN status VARCHAR(50) NOT NULL DEFAULT 'PAID';
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'bill' AND column_name = 'refunded_at') THEN
        ALTER TABLE bill ADD COLUMN refunded_at TIMESTAMPTZ;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'bill' AND column_name = 'refund_reason') THEN
        ALTER TABLE bill ADD COLUMN refund_reason TEXT;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'bill' AND column_name = 'refunded_amount') THEN
        ALTER TABLE bill ADD COLUMN refunded_amount DECIMAL(12,2) DEFAULT 0;
    END IF;
END $$;

-- 2. Add created_at timestamptz to billing_transaction
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'billing_transaction' AND column_name = 'created_at') THEN
        ALTER TABLE billing_transaction ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
    END IF;
END $$;

-- Migrate existing data from billing_date to created_at
UPDATE billing_transaction SET created_at = billing_date AT TIME ZONE 'UTC' WHERE created_at IS NULL;
