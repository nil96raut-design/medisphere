-- ============================================================
-- V11: Fix tenant isolation for prescription_item & nursing_log
-- ============================================================

-- 1. Add hospital_id to prescription_item
ALTER TABLE prescription_item ADD COLUMN hospital_id BIGINT NOT NULL;
ALTER TABLE prescription_item ADD CONSTRAINT fk_prescription_item_hospital
    FOREIGN KEY (hospital_id) REFERENCES hospital(id);

CREATE INDEX idx_prescription_item_hospital ON prescription_item(hospital_id);

-- 2. Add hospital_id to nursing_log
ALTER TABLE nursing_log ADD COLUMN hospital_id BIGINT NOT NULL;
ALTER TABLE nursing_log ADD CONSTRAINT fk_nursing_log_hospital
    FOREIGN KEY (hospital_id) REFERENCES hospital(id);

CREATE INDEX idx_nursing_log_hospital ON nursing_log(hospital_id);

-- 3. Add ON DELETE CASCADE to child FK constraints
-- prescription_item → medical_record
ALTER TABLE prescription_item DROP CONSTRAINT fk_prescription_item_medical_record;
ALTER TABLE prescription_item ADD CONSTRAINT fk_prescription_item_medical_record
    FOREIGN KEY (medical_record_id) REFERENCES medical_record(id) ON DELETE CASCADE;

-- nursing_log → admission
ALTER TABLE nursing_log DROP CONSTRAINT fk_nursing_log_admission;
ALTER TABLE nursing_log ADD CONSTRAINT fk_nursing_log_admission
    FOREIGN KEY (admission_id) REFERENCES admission(id) ON DELETE CASCADE;

-- progress_note → task
ALTER TABLE progress_note DROP CONSTRAINT IF EXISTS fk_progress_note_task;
ALTER TABLE progress_note ADD CONSTRAINT fk_progress_note_task
    FOREIGN KEY (task_id) REFERENCES task(id) ON DELETE CASCADE;

-- dispensation_record → medicine_stock
ALTER TABLE dispensation_record DROP CONSTRAINT IF EXISTS fk_dispensation_record_medicine_stock;
ALTER TABLE dispensation_record ADD CONSTRAINT fk_dispensation_record_medicine_stock
    FOREIGN KEY (medicine_stock_id) REFERENCES medicine_stock(id) ON DELETE CASCADE;

-- admission → bed
ALTER TABLE admission DROP CONSTRAINT IF EXISTS fk_admission_bed;
ALTER TABLE admission ADD CONSTRAINT fk_admission_bed
    FOREIGN KEY (bed_id) REFERENCES bed(id) ON DELETE SET NULL;

-- service_request → medical_record
ALTER TABLE service_request DROP CONSTRAINT IF EXISTS fk_service_request_medical_record;
ALTER TABLE service_request ADD CONSTRAINT fk_service_request_medical_record
    FOREIGN KEY (medical_record_id) REFERENCES medical_record(id) ON DELETE CASCADE;
