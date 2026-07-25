-- ============================================================
-- V11: Fix tenant isolation for prescription_item & nursing_log
-- ============================================================
-- FULLY IDEMPOTENT. Safe on fresh DB, partial migration, re-deploy.
--
-- For each FK rebuild we:
--   1. Dynamically drop ANY existing constraint on the FK column
--      (handles both auto-generated PG names and custom names)
--   2. Re-add with desired ON DELETE behaviour under a fixed name
--   3. All wrapped in pg_constraint + information_schema catalog checks
-- ============================================================

-- ___________________________________________________________________
-- 1. Add hospital_id to prescription_item (idempotent)
-- ___________________________________________________________________

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'prescription_item' AND column_name = 'hospital_id'
    ) THEN
        ALTER TABLE prescription_item ADD COLUMN hospital_id BIGINT;
    END IF;
END $$;

UPDATE prescription_item SET hospital_id = (SELECT id FROM hospital ORDER BY id LIMIT 1) WHERE hospital_id IS NULL;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'prescription_item' AND column_name = 'hospital_id'
          AND is_nullable = 'YES'
    ) THEN
        ALTER TABLE prescription_item ALTER COLUMN hospital_id SET NOT NULL;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_prescription_item_hospital') THEN
        ALTER TABLE prescription_item ADD CONSTRAINT fk_prescription_item_hospital
            FOREIGN KEY (hospital_id) REFERENCES hospital(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_prescription_item_hospital ON prescription_item(hospital_id);

-- ___________________________________________________________________
-- 2. Add hospital_id to nursing_log (idempotent)
-- ___________________________________________________________________

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'nursing_log' AND column_name = 'hospital_id'
    ) THEN
        ALTER TABLE nursing_log ADD COLUMN hospital_id BIGINT;
    END IF;
END $$;

UPDATE nursing_log SET hospital_id = (SELECT id FROM hospital ORDER BY id LIMIT 1) WHERE hospital_id IS NULL;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'nursing_log' AND column_name = 'hospital_id'
          AND is_nullable = 'YES'
    ) THEN
        ALTER TABLE nursing_log ALTER COLUMN hospital_id SET NOT NULL;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_nursing_log_hospital') THEN
        ALTER TABLE nursing_log ADD CONSTRAINT fk_nursing_log_hospital
            FOREIGN KEY (hospital_id) REFERENCES hospital(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_nursing_log_hospital ON nursing_log(hospital_id);

-- ___________________________________________________________________
-- 3. Rebuild FK: prescription_item → medical_record (ON DELETE CASCADE)
--    Original created inline in V6 → auto-name: prescription_item_medical_record_id_fkey
--    V11 tried custom name: fk_prescription_item_medical_record (wrong → crash)
-- ===================================================================

DO $$
DECLARE r RECORD;
BEGIN
    FOR r IN (
        SELECT conname FROM pg_constraint
        WHERE conrelid = 'prescription_item'::regclass
          AND contype = 'f'
          AND (conname LIKE '%medical_record%' OR conname = 'fk_prescription_item_medical_record')
    ) LOOP
        EXECUTE 'ALTER TABLE prescription_item DROP CONSTRAINT ' || quote_ident(r.conname);
    END LOOP;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_prescription_item_medical_record') THEN
        ALTER TABLE prescription_item ADD CONSTRAINT fk_prescription_item_medical_record
            FOREIGN KEY (medical_record_id) REFERENCES medical_record(id) ON DELETE CASCADE;
    END IF;
END $$;

-- ___________________________________________________________________
-- 4. Rebuild FK: nursing_log → admission (ON DELETE CASCADE)
--    Original inline in V7 → auto-name: nursing_log_admission_id_fkey
--    V11 tried: fk_nursing_log_admission (wrong → crash)
-- ===================================================================

DO $$
DECLARE r RECORD;
BEGIN
    FOR r IN (
        SELECT conname FROM pg_constraint
        WHERE conrelid = 'nursing_log'::regclass
          AND contype = 'f'
          AND (conname LIKE '%admission%' OR conname = 'fk_nursing_log_admission')
    ) LOOP
        EXECUTE 'ALTER TABLE nursing_log DROP CONSTRAINT ' || quote_ident(r.conname);
    END LOOP;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_nursing_log_admission') THEN
        ALTER TABLE nursing_log ADD CONSTRAINT fk_nursing_log_admission
            FOREIGN KEY (admission_id) REFERENCES admission(id) ON DELETE CASCADE;
    END IF;
END $$;

-- ___________________________________________________________________
-- 5. Rebuild FK: progress_note → task (ON DELETE CASCADE)
--    Original inline in V1 → auto-name: progress_note_task_id_fkey
--    V11 tried: fk_progress_note_task (IF EXISTS → silent no-op + duplicate)
-- ===================================================================

DO $$
DECLARE r RECORD;
BEGIN
    FOR r IN (
        SELECT conname FROM pg_constraint
        WHERE conrelid = 'progress_note'::regclass
          AND contype = 'f'
          AND (conname LIKE '%task%' OR conname = 'fk_progress_note_task')
    ) LOOP
        EXECUTE 'ALTER TABLE progress_note DROP CONSTRAINT ' || quote_ident(r.conname);
    END LOOP;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_progress_note_task') THEN
        ALTER TABLE progress_note ADD CONSTRAINT fk_progress_note_task
            FOREIGN KEY (task_id) REFERENCES task(id) ON DELETE CASCADE;
    END IF;
END $$;

-- ___________________________________________________________________
-- 6. Rebuild FK: dispensation_record → medicine_stock (ON DELETE CASCADE)
--    Original inline in V9 → auto-name: dispensation_record_medicine_stock_id_fkey
--    V11 tried: fk_dispensation_record_medicine_stock (IF EXISTS → silent no-op + duplicate)
-- ===================================================================

DO $$
DECLARE r RECORD;
BEGIN
    FOR r IN (
        SELECT conname FROM pg_constraint
        WHERE conrelid = 'dispensation_record'::regclass
          AND contype = 'f'
          AND (conname LIKE '%medicine_stock%' OR conname = 'fk_dispensation_record_medicine_stock')
    ) LOOP
        EXECUTE 'ALTER TABLE dispensation_record DROP CONSTRAINT ' || quote_ident(r.conname);
    END LOOP;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_dispensation_record_medicine_stock') THEN
        ALTER TABLE dispensation_record ADD CONSTRAINT fk_dispensation_record_medicine_stock
            FOREIGN KEY (medicine_stock_id) REFERENCES medicine_stock(id) ON DELETE CASCADE;
    END IF;
END $$;

-- ___________________________________________________________________
-- 7. Rebuild FK: admission → bed (ON DELETE SET NULL)
--    Original inline in V7 → auto-name: admission_bed_id_fkey
--    V11 tried: fk_admission_bed (IF EXISTS → silent no-op + duplicate)
-- ===================================================================

DO $$
DECLARE r RECORD;
BEGIN
    FOR r IN (
        SELECT conname FROM pg_constraint
        WHERE conrelid = 'admission'::regclass
          AND contype = 'f'
          AND (conname LIKE '%bed%' OR conname = 'fk_admission_bed')
    ) LOOP
        EXECUTE 'ALTER TABLE admission DROP CONSTRAINT ' || quote_ident(r.conname);
    END LOOP;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_admission_bed') THEN
        ALTER TABLE admission ADD CONSTRAINT fk_admission_bed
            FOREIGN KEY (bed_id) REFERENCES bed(id) ON DELETE SET NULL;
    END IF;
END $$;

-- ___________________________________________________________________
-- 8. Rebuild FK: service_request → medical_record (ON DELETE CASCADE)
--    Original inline in V6 → auto-name: service_request_medical_record_id_fkey
--    V11 tried: fk_service_request_medical_record (IF EXISTS → silent no-op + duplicate)
-- ===================================================================

DO $$
DECLARE r RECORD;
BEGIN
    FOR r IN (
        SELECT conname FROM pg_constraint
        WHERE conrelid = 'service_request'::regclass
          AND contype = 'f'
          AND (conname LIKE '%medical_record%' OR conname = 'fk_service_request_medical_record')
    ) LOOP
        EXECUTE 'ALTER TABLE service_request DROP CONSTRAINT ' || quote_ident(r.conname);
    END LOOP;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_service_request_medical_record') THEN
        ALTER TABLE service_request ADD CONSTRAINT fk_service_request_medical_record
            FOREIGN KEY (medical_record_id) REFERENCES medical_record(id) ON DELETE CASCADE;
    END IF;
END $$;
