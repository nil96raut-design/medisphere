CREATE TABLE IF NOT EXISTS feature_flags (
    id BIGSERIAL PRIMARY KEY,
    feature_name VARCHAR(100) NOT NULL UNIQUE,
    is_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO feature_flags (feature_name, is_enabled, description)
VALUES 
    ('CHATBOT_ENABLED', true, 'Enable AI Chatbot service'),
    ('CHAOS_MONKEY_ENABLED', false, 'Enable Chaos Engineering fault injection'),
    ('AUTO_NURSE_ASSIGNMENT', true, 'Automatically assign nurses upon IPD admission')
ON CONFLICT (feature_name) DO NOTHING;
