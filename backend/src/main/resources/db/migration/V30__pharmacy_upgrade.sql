-- ============================================================
-- V30: Pharmacy Enterprise Upgrade
-- - MedicineStock: purchase_price, selling_price, quantity_reserved
-- - Supplier management
-- - Enhanced purchase_order
-- - DispensationRecord: dispensation_status, batch_id, remaining_quantity
-- - ExpiryAlert system
-- - Performance indexes
-- ============================================================
-- FULLY IDEMPOTENT.

-- 1. Supplier table (Must be created before references)
CREATE TABLE IF NOT EXISTS supplier (
    id BIGSERIAL PRIMARY KEY,
    hospital_id BIGINT NOT NULL REFERENCES hospital(id),
    name VARCHAR(255) NOT NULL,
    contact_person VARCHAR(255),
    contact_number VARCHAR(50),
    email VARCHAR(255),
    address TEXT,
    gst_number VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_supplier_hospital ON supplier(hospital_id);

-- 2. Augment medicine_stock
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'medicine_stock' AND column_name = 'purchase_price') THEN
        ALTER TABLE medicine_stock ADD COLUMN purchase_price DECIMAL(10,2) DEFAULT 0;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'medicine_stock' AND column_name = 'selling_price') THEN
        ALTER TABLE medicine_stock ADD COLUMN selling_price DECIMAL(10,2) DEFAULT 0;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'medicine_stock' AND column_name = 'supplier_id') THEN
        ALTER TABLE medicine_stock ADD COLUMN supplier_id BIGINT REFERENCES supplier(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'medicine_stock' AND column_name = 'quantity_reserved') THEN
        ALTER TABLE medicine_stock ADD COLUMN quantity_reserved INTEGER NOT NULL DEFAULT 0;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'medicine_stock' AND column_name = 'created_at') THEN
        ALTER TABLE medicine_stock ADD COLUMN created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW();
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_supplier_hospital ON supplier(hospital_id);

-- 3. Enhance purchase_order (add supplier_id, medicine_name, quantity_received, total_price columns)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'purchase_order' AND column_name = 'supplier_id') THEN
        ALTER TABLE purchase_order ADD COLUMN supplier_id BIGINT REFERENCES supplier(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'purchase_order' AND column_name = 'medicine_name') THEN
        ALTER TABLE purchase_order ADD COLUMN medicine_name VARCHAR(255);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'purchase_order' AND column_name = 'quantity_received') THEN
        ALTER TABLE purchase_order ADD COLUMN quantity_received INTEGER DEFAULT 0;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'purchase_order' AND column_name = 'total_price') THEN
        ALTER TABLE purchase_order ADD COLUMN total_price DECIMAL(10,2);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'purchase_order' AND column_name = 'unit_price') THEN
        ALTER TABLE purchase_order ADD COLUMN unit_price DECIMAL(10,2);
    END IF;
END $$;

-- 4. DispensationRecord upgrade columns
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'dispensation_record' AND column_name = 'dispensation_status') THEN
        ALTER TABLE dispensation_record ADD COLUMN dispensation_status VARCHAR(20) DEFAULT 'COMPLETE';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'dispensation_record' AND column_name = 'batch_id') THEN
        ALTER TABLE dispensation_record ADD COLUMN batch_id BIGINT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'dispensation_record' AND column_name = 'remaining_quantity') THEN
        ALTER TABLE dispensation_record ADD COLUMN remaining_quantity INTEGER DEFAULT 0;
    END IF;
END $$;

-- 5. ExpiryAlert table
CREATE TABLE IF NOT EXISTS expiry_alert (
    id BIGSERIAL PRIMARY KEY,
    hospital_id BIGINT NOT NULL REFERENCES hospital(id),
    medicine_stock_id BIGINT NOT NULL REFERENCES medicine_stock(id),
    medicine_name VARCHAR(255) NOT NULL,
    batch_number VARCHAR(255) NOT NULL,
    expiry_date DATE NOT NULL,
    alert_type VARCHAR(20) NOT NULL,
    is_resolved BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    resolved_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_expiry_alert_hospital ON expiry_alert(hospital_id);
CREATE INDEX IF NOT EXISTS idx_expiry_alert_type ON expiry_alert(alert_type);
CREATE INDEX IF NOT EXISTS idx_expiry_alert_resolved ON expiry_alert(is_resolved);

-- 6. Performance indexes for pharmacy
CREATE INDEX IF NOT EXISTS idx_dispensation_status ON dispensation_record(dispensation_status);
CREATE INDEX IF NOT EXISTS idx_stock_quantity ON medicine_stock(available_quantity);
CREATE INDEX IF NOT EXISTS idx_stock_reserved ON medicine_stock(quantity_reserved);
