CREATE TABLE IF NOT EXISTS user_session (
    id BIGSERIAL PRIMARY KEY,
    session_token VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    hospital_id BIGINT NOT NULL,
    ip_address VARCHAR(45),
    user_agent TEXT,
    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_active_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_session_user ON user_session(user_id, is_revoked);
