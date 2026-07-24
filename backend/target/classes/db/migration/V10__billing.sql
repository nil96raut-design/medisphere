CREATE TABLE billing_transaction (
    id BIGSERIAL PRIMARY KEY,
    hospital_id BIGINT NOT NULL REFERENCES hospital(id),
    patient_id BIGINT NOT NULL REFERENCES patient(id),
    admission_id BIGINT REFERENCES admission(id),
    appointment_id BIGINT REFERENCES appointment(id),
    item_description VARCHAR(255) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    billing_date TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_bt_hospital ON billing_transaction(hospital_id);
CREATE INDEX idx_bt_patient ON billing_transaction(patient_id);

CREATE TABLE bill (
    id BIGSERIAL PRIMARY KEY,
    hospital_id BIGINT NOT NULL REFERENCES hospital(id),
    patient_id BIGINT NOT NULL REFERENCES patient(id),
    total_amount DECIMAL(12, 2) NOT NULL,
    discount_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    insurance_covered_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    net_payable DECIMAL(12, 2) NOT NULL,
    payment_status VARCHAR(50) NOT NULL DEFAULT 'UNPAID',
    payment_mode VARCHAR(50),
    final_invoice_pdf_path VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_bill_hospital ON bill(hospital_id);
CREATE INDEX idx_bill_patient ON bill(patient_id);
CREATE INDEX idx_bill_status ON bill(payment_status);
