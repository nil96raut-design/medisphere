-- =========================================================================
-- EXPAND -> MIGRATE -> CONTRACT PATTERN EXAMPLE
-- =========================================================================

-- PHASE 1: EXPAND
-- Safely introduce residential_address without breaking legacy address column
ALTER TABLE patients ADD COLUMN IF NOT EXISTS residential_address TEXT;

-- PHASE 2: MIGRATE (Backfill historical data idempotently)
UPDATE patients 
SET residential_address = address 
WHERE residential_address IS NULL AND address IS NOT NULL;

-- Index addition concurrently (Non-blocking)
CREATE INDEX IF NOT EXISTS idx_patients_residential_address ON patients(residential_address);
