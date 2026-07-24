CREATE TABLE medicine_stock (
    id BIGSERIAL PRIMARY KEY,
    hospital_id BIGINT NOT NULL REFERENCES hospital(id),
    medicine_name VARCHAR(255) NOT NULL,
    batch_number VARCHAR(255) NOT NULL,
    expiry_date DATE NOT NULL,
    available_quantity INTEGER NOT NULL DEFAULT 0,
    reorder_level INTEGER NOT NULL DEFAULT 10,
    unit_price DECIMAL(10, 2) NOT NULL DEFAULT 0
);

CREATE INDEX idx_medicine_stock_hospital ON medicine_stock(hospital_id);
CREATE INDEX idx_medicine_stock_name ON medicine_stock(medicine_name);
CREATE UNIQUE INDEX idx_medicine_batch ON medicine_stock(hospital_id, medicine_name, batch_number);

CREATE TABLE dispensation_record (
    id BIGSERIAL PRIMARY KEY,
    hospital_id BIGINT NOT NULL REFERENCES hospital(id),
    patient_id BIGINT NOT NULL REFERENCES patient(id),
    prescription_item_id BIGINT REFERENCES prescription_item(id),
    medicine_stock_id BIGINT REFERENCES medicine_stock(id),
    medicine_name VARCHAR(255) NOT NULL,
    quantity_dispensed INTEGER NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL DEFAULT 0,
    total_price DECIMAL(10, 2) NOT NULL DEFAULT 0,
    billing_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    dispensed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    dispensed_by BIGINT NOT NULL REFERENCES app_user(id)
);

CREATE INDEX idx_dispensation_hospital ON dispensation_record(hospital_id);
CREATE INDEX idx_dispensation_patient ON dispensation_record(patient_id);
CREATE INDEX idx_dispensation_prescription ON dispensation_record(prescription_item_id);
CREATE INDEX idx_dispensation_status ON dispensation_record(billing_status);
