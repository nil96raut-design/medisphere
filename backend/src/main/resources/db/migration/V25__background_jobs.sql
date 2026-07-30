-- ============================================================
-- V25: Background job tracking
-- ============================================================
-- FULLY IDEMPOTENT.

CREATE TABLE IF NOT EXISTS background_job (
    id BIGSERIAL PRIMARY KEY,
    job_type VARCHAR(100) NOT NULL,
    payload TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    priority INTEGER NOT NULL DEFAULT 5,
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 3,
    last_error TEXT,
    next_attempt_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    locked_until TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_bj_status ON background_job(status, next_attempt_at);
CREATE INDEX IF NOT EXISTS idx_bj_type ON background_job(job_type);
