CREATE TABLE IF NOT EXISTS patient_consent (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    granted_role VARCHAR(50) NOT NULL,
    consent_type VARCHAR(50) NOT NULL,
    is_granted BOOLEAN NOT NULL DEFAULT TRUE,
    granted_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE,
    notes TEXT
);

CREATE INDEX IF NOT EXISTS idx_patient_consent_lookup ON patient_consent(patient_id, granted_role);
