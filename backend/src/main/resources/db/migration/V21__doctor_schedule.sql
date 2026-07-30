-- ============================================================
-- V21: Doctor schedule / availability system
-- ============================================================
-- FULLY IDEMPOTENT.

CREATE TABLE IF NOT EXISTS doctor_schedule (
    id BIGSERIAL PRIMARY KEY,
    doctor_id BIGINT NOT NULL REFERENCES doctor(id),
    hospital_id BIGINT NOT NULL REFERENCES hospital(id),
    day_of_week SMALLINT NOT NULL CHECK (day_of_week BETWEEN 0 AND 6),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_doctor_schedule UNIQUE (doctor_id, day_of_week, start_time)
);

CREATE INDEX IF NOT EXISTS idx_ds_doctor ON doctor_schedule(doctor_id);
CREATE INDEX IF NOT EXISTS idx_ds_hospital ON doctor_schedule(hospital_id);
CREATE INDEX IF NOT EXISTS idx_ds_day ON doctor_schedule(day_of_week);

ALTER TABLE doctor ADD COLUMN IF NOT EXISTS consultation_duration_minutes INTEGER DEFAULT 15;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'doctor' AND column_name = 'qualification') THEN
        ALTER TABLE doctor ADD COLUMN qualification VARCHAR(255);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'doctor' AND column_name = 'license_number') THEN
        ALTER TABLE doctor ADD COLUMN license_number VARCHAR(100);
    END IF;
END $$;
