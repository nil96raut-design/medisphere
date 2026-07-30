-- ============================================================
-- V22: BilledItem table — hard double-billing prevention
-- ============================================================
-- FULLY IDEMPOTENT.

CREATE TABLE IF NOT EXISTS billed_item (
    id BIGSERIAL PRIMARY KEY,
    hospital_id BIGINT NOT NULL REFERENCES hospital(id),
    patient_id BIGINT NOT NULL REFERENCES patient(id),
    bill_id BIGINT NOT NULL REFERENCES bill(id),
    source_type VARCHAR(50) NOT NULL,
    source_id BIGINT NOT NULL,
    amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_billed_source UNIQUE (source_type, source_id)
);

CREATE INDEX IF NOT EXISTS idx_bi_hospital ON billed_item(hospital_id);
CREATE INDEX IF NOT EXISTS idx_bi_patient ON billed_item(patient_id);
CREATE INDEX IF NOT EXISTS idx_bi_bill ON billed_item(bill_id);
CREATE INDEX IF NOT EXISTS idx_bi_source ON billed_item(source_type, source_id);
