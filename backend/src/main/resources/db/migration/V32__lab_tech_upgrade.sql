ALTER TABLE lab_test_order ADD COLUMN IF NOT EXISTS processing_started_at TIMESTAMP;
ALTER TABLE lab_test_order ADD COLUMN IF NOT EXISTS processing_completed_at TIMESTAMP;
ALTER TABLE lab_test_order ADD COLUMN IF NOT EXISTS result_entered_by BIGINT REFERENCES app_user(id);
ALTER TABLE lab_test_order ADD COLUMN IF NOT EXISTS retest_of BIGINT REFERENCES lab_test_order(id);
ALTER TABLE lab_test_order ADD COLUMN IF NOT EXISTS correction_reason TEXT;
ALTER TABLE lab_test_order ADD COLUMN IF NOT EXISTS turnaround_minutes INTEGER;
ALTER TABLE lab_test_order ADD COLUMN IF NOT EXISTS critical_flag BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE lab_test_order ADD COLUMN IF NOT EXISTS sample_barcode VARCHAR(100);
ALTER TABLE lab_test_order ADD COLUMN IF NOT EXISTS sample_storage_location VARCHAR(255);

CREATE TABLE IF NOT EXISTS lab_critical_rule (
    id BIGSERIAL PRIMARY KEY,
    hospital_id BIGINT NOT NULL REFERENCES hospital(id),
    test_name VARCHAR(255) NOT NULL,
    parameter_name VARCHAR(255) NOT NULL,
    condition_operator VARCHAR(20) NOT NULL,
    threshold_value VARCHAR(255) NOT NULL,
    unit VARCHAR(100),
    severity VARCHAR(20) NOT NULL DEFAULT 'HIGH',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS sample_tracking (
    id BIGSERIAL PRIMARY KEY,
    hospital_id BIGINT NOT NULL REFERENCES hospital(id),
    lab_order_id BIGINT NOT NULL REFERENCES lab_test_order(id),
    sample_type VARCHAR(100) NOT NULL,
    container_type VARCHAR(100),
    barcode VARCHAR(100),
    collection_volume VARCHAR(50),
    collection_method VARCHAR(100),
    storage_location VARCHAR(255),
    storage_condition VARCHAR(100),
    collected_by BIGINT NOT NULL REFERENCES app_user(id),
    collected_at TIMESTAMP NOT NULL DEFAULT NOW(),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_lab_order_processing ON lab_test_order(hospital_id, status) WHERE status IN ('ORDERED', 'SAMPLE_COLLECTED', 'PROCESSING');
CREATE INDEX IF NOT EXISTS idx_lab_order_critical ON lab_test_order(hospital_id, critical_flag) WHERE critical_flag = TRUE;
CREATE INDEX IF NOT EXISTS idx_lab_order_retest ON lab_test_order(retest_of);
CREATE INDEX IF NOT EXISTS idx_lab_order_turnaround ON lab_test_order(hospital_id, turnaround_minutes);
CREATE INDEX IF NOT EXISTS idx_lab_critical_rule_hospital ON lab_critical_rule(hospital_id, test_name);
CREATE INDEX IF NOT EXISTS idx_sample_tracking_order ON sample_tracking(lab_order_id);
CREATE INDEX IF NOT EXISTS idx_sample_tracking_barcode ON sample_tracking(barcode);
