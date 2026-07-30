-- ============================================================
-- V24: Insurance claim workflow
-- ============================================================
-- FULLY IDEMPOTENT.

CREATE TABLE IF NOT EXISTS insurance_claim (
    id BIGSERIAL PRIMARY KEY,
    bill_id BIGINT NOT NULL REFERENCES bill(id),
    hospital_id BIGINT NOT NULL REFERENCES hospital(id),
    patient_id BIGINT NOT NULL REFERENCES patient(id),
    claim_amount NUMERIC(12,2) NOT NULL,
    approved_amount NUMERIC(12,2),
    status VARCHAR(30) NOT NULL DEFAULT 'INITIATED',
    insurer_name VARCHAR(255),
    policy_number VARCHAR(100),
    submitted_at TIMESTAMPTZ,
    approved_at TIMESTAMPTZ,
    rejected_at TIMESTAMPTZ,
    rejection_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_ic_bill ON insurance_claim(bill_id);
CREATE INDEX IF NOT EXISTS idx_ic_hospital ON insurance_claim(hospital_id);
CREATE INDEX IF NOT EXISTS idx_ic_status ON insurance_claim(status);
