-- Baseline schema, mirrors the entities in com.healthtrack.entity as of the
-- last ddl-auto: update run. Keep this file append-only once shipped:
-- add V2__..., V3__... for future changes rather than editing this one.

CREATE TABLE app_user (
    id              BIGSERIAL PRIMARY KEY,
    full_name       VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(32)  NOT NULL,
    primary_doctor_id BIGINT REFERENCES app_user(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_app_user_primary_doctor ON app_user(primary_doctor_id);
CREATE INDEX idx_app_user_role ON app_user(role);

CREATE TABLE task (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(255) NOT NULL,
    description     VARCHAR(2000),
    assignee_id     BIGINT NOT NULL REFERENCES app_user(id),
    assigned_by_id  BIGINT NOT NULL REFERENCES app_user(id),
    status          VARCHAR(32) NOT NULL DEFAULT 'NOT_STARTED',
    priority        VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
    progress_percent INTEGER NOT NULL DEFAULT 0,
    due_date        DATE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ
);

CREATE INDEX idx_task_assignee ON task(assignee_id);
CREATE INDEX idx_task_assigned_by ON task(assigned_by_id);
CREATE INDEX idx_task_status ON task(status);
CREATE INDEX idx_task_due_date ON task(due_date);
-- supports the upcoming ILIKE search on title/description
CREATE INDEX idx_task_title ON task(lower(title));

CREATE TABLE progress_note (
    id              BIGSERIAL PRIMARY KEY,
    task_id         BIGINT NOT NULL REFERENCES task(id) ON DELETE CASCADE,
    author_id       BIGINT NOT NULL REFERENCES app_user(id),
    note            VARCHAR(2000),
    progress_percent INTEGER,
    status          VARCHAR(32),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_progress_note_task ON progress_note(task_id);
