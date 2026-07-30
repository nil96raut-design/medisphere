CREATE TABLE write_buffer (
    id BIGSERIAL PRIMARY KEY,
    operation_type VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    priority VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_write_buffer_status_priority_created_at ON write_buffer(status, priority, created_at);
