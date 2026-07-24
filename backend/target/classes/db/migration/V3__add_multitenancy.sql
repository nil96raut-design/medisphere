-- Add multi-tenancy tables
CREATE TABLE hospital (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    license_number VARCHAR(255) NOT NULL UNIQUE,
    contact_email VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    subscription_tier VARCHAR(32) NOT NULL,
    subscription_status VARCHAR(32) NOT NULL,
    trial_end_date TIMESTAMPTZ
);

CREATE TABLE subscription (
    id BIGSERIAL PRIMARY KEY,
    hospital_id BIGINT NOT NULL REFERENCES hospital(id) ON DELETE CASCADE,
    plan_type VARCHAR(32) NOT NULL,
    amount_paid DECIMAL(10,2) NOT NULL,
    start_date TIMESTAMPTZ NOT NULL,
    end_date TIMESTAMPTZ NOT NULL,
    payment_status VARCHAR(32) NOT NULL
);

CREATE INDEX idx_subscription_hospital ON subscription(hospital_id);

-- Backfill data to a default hospital to prevent data loss
INSERT INTO hospital (name, license_number, contact_email, subscription_tier, subscription_status)
VALUES ('Default Hospital', 'DEFAULT-001', 'admin@healthtrack.dev', 'MONTHLY', 'ACTIVE');

-- Add hospital_id to existing tenant-scoped entities
ALTER TABLE app_user ADD COLUMN hospital_id BIGINT REFERENCES hospital(id);
ALTER TABLE task ADD COLUMN hospital_id BIGINT REFERENCES hospital(id);
ALTER TABLE progress_note ADD COLUMN hospital_id BIGINT REFERENCES hospital(id);

-- Assign existing data to Default Hospital
UPDATE app_user SET hospital_id = (SELECT id FROM hospital WHERE license_number = 'DEFAULT-001');
UPDATE task SET hospital_id = (SELECT id FROM hospital WHERE license_number = 'DEFAULT-001');
UPDATE progress_note SET hospital_id = (SELECT id FROM hospital WHERE license_number = 'DEFAULT-001');

-- Enforce strict NOT NULL constraints
ALTER TABLE app_user ALTER COLUMN hospital_id SET NOT NULL;
ALTER TABLE task ALTER COLUMN hospital_id SET NOT NULL;
ALTER TABLE progress_note ALTER COLUMN hospital_id SET NOT NULL;

CREATE INDEX idx_app_user_hospital ON app_user(hospital_id);
CREATE INDEX idx_task_hospital ON task(hospital_id);
CREATE INDEX idx_progress_note_hospital ON progress_note(hospital_id);
