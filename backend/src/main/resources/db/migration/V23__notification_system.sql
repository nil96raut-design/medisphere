-- ============================================================
-- V23: Notification system
-- ============================================================
-- FULLY IDEMPOTENT.

CREATE TABLE IF NOT EXISTS notification (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_user(id),
    hospital_id BIGINT NOT NULL REFERENCES hospital(id),
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    channel VARCHAR(20) NOT NULL DEFAULT 'IN_APP',
    reference_type VARCHAR(50),
    reference_id BIGINT,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    sent_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_notif_user ON notification(user_id);
CREATE INDEX IF NOT EXISTS idx_notif_hospital ON notification(hospital_id);
CREATE INDEX IF NOT EXISTS idx_notif_status ON notification(status);
CREATE INDEX IF NOT EXISTS idx_notif_created ON notification(created_at);
