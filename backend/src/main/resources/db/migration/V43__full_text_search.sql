-- FULL-TEXT SEARCH INDEXES FOR GLOBAL SEARCH
CREATE INDEX IF NOT EXISTS idx_fts_patients ON patients USING gin(to_tsvector('english', coalesce(first_name, '') || ' ' || coalesce(last_name, '') || ' ' || coalesce(phone_number, '')));
CREATE INDEX IF NOT EXISTS idx_fts_lab_orders ON lab_test_order USING gin(to_tsvector('english', coalesce(test_name, '') || ' ' || coalesce(technician_notes, '')));
