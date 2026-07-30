-- ============================================================
-- V19: Pharmacy expiry tracking + reorder system
-- ============================================================
-- FULLY IDEMPOTENT.

-- 1. Add last_reordered_at to track reorder activity on medicine_stock
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'medicine_stock' AND column_name = 'last_reordered_at') THEN
        ALTER TABLE medicine_stock ADD COLUMN last_reordered_at TIMESTAMPTZ;
    END IF;
END $$;

-- 2. Create purchase_order table for reorder system
CREATE TABLE IF NOT EXISTS purchase_order (
    id BIGSERIAL PRIMARY KEY,
    hospital_id BIGINT NOT NULL REFERENCES hospital(id),
    medicine_stock_id BIGINT NOT NULL REFERENCES medicine_stock(id),
    quantity INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    received_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_po_hospital ON purchase_order(hospital_id);
CREATE INDEX IF NOT EXISTS idx_po_status ON purchase_order(status);

-- 3. Indexes for expiry alerts
CREATE INDEX IF NOT EXISTS idx_stock_expiry ON medicine_stock(expiry_date);
CREATE INDEX IF NOT EXISTS idx_stock_reorder ON medicine_stock(hospital_id, available_quantity, reorder_level);
