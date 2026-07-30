ALTER TABLE app_user ADD COLUMN IF NOT EXISTS refresh_token_version INT DEFAULT 0;
