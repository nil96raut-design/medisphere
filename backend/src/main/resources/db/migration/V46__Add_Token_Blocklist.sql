CREATE TABLE IF NOT EXISTS token_blocklist (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(1000) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    blocked_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_token_blocklist_token ON token_blocklist(token);
