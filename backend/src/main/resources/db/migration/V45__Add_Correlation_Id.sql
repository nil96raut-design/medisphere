ALTER TABLE audit_log ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(255);
CREATE INDEX IF NOT EXISTS idx_audit_correlation_id ON audit_log(correlation_id);
