-- ============================================================
-- V15: Billing transaction line items + transaction type
-- ============================================================
-- FULLY IDEMPOTENT. Safe on fresh DB, partial migration, re-deploy.

-- ___________________________________________________________________
-- 1. Add bill_id FK to billing_transaction
-- ___________________________________________________________________

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'billing_transaction' AND column_name = 'bill_id'
    ) THEN
        ALTER TABLE billing_transaction ADD COLUMN bill_id BIGINT;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_bt_bill'
    ) THEN
        ALTER TABLE billing_transaction ADD CONSTRAINT fk_bt_bill
            FOREIGN KEY (bill_id) REFERENCES bill(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_bt_bill ON billing_transaction(bill_id);

-- ___________________________________________________________________
-- 2. Add transaction_type column
-- ___________________________________________________________________

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'billing_transaction' AND column_name = 'transaction_type'
    ) THEN
        ALTER TABLE billing_transaction ADD COLUMN transaction_type VARCHAR(50) NOT NULL DEFAULT 'OTHER';
    END IF;
END $$;
