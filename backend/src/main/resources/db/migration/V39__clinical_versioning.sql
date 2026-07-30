CREATE TABLE IF NOT EXISTS clinical_record_history (
    id BIGSERIAL PRIMARY KEY,
    record_type VARCHAR(50) NOT NULL,
    record_id BIGINT NOT NULL,
    version_number INT NOT NULL,
    snapshot_payload JSONB NOT NULL,
    modified_by VARCHAR(100) NOT NULL,
    modified_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    change_reason TEXT
);

CREATE INDEX IF NOT EXISTS idx_clinical_history_lookup ON clinical_record_history(record_type, record_id);
