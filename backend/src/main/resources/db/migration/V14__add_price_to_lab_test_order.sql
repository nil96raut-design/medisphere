-- ============================================================
-- V14: Add price column to lab_test_order
-- ============================================================
-- FULLY IDEMPOTENT. Safe on fresh DB, partial migration, re-deploy.
-- ============================================================

-- Step 1: Add column as nullable (only if missing)
--   Why: ALTER TABLE ADD COLUMN on an existing column crashes.
--        IF NOT EXISTS prevents re-run failures.
--        Adding nullable first avoids NOT NULL violation on existing rows.
-- ============================================================
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'lab_test_order' AND column_name = 'price'
    ) THEN
        ALTER TABLE lab_test_order ADD COLUMN price DECIMAL(10,2);
    END IF;
END $$;

-- Step 2: Backfill existing rows with a default value
--   Why: New column will be NOT NULL. Existing rows have NULL after ADD COLUMN.
--        Setting to 0.00 preserves data integrity without guessing real prices.
--        Where clause limits UPDATE to only rows that still need a value.
-- ============================================================
UPDATE lab_test_order SET price = 0.00 WHERE price IS NULL;

-- Step 3: Enforce NOT NULL (only if still nullable)
--   Why: ALTER COLUMN SET NOT NULL on an already-NOT-NULL column is safe
--        but guarded to avoid any edge-case issues.
-- ============================================================
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'lab_test_order' AND column_name = 'price'
          AND is_nullable = 'YES'
    ) THEN
        ALTER TABLE lab_test_order ALTER COLUMN price SET NOT NULL;
    END IF;
END $$;
