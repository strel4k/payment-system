CREATE TABLE rate_correction_factors (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP DEFAULT NOW() NOT NULL,
    modified_at TIMESTAMP,
    source_code VARCHAR(10) NOT NULL,
    destination_code VARCHAR(10) NOT NULL,
    factor NUMERIC(10, 6) NOT NULL DEFAULT 1.0,
    description VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_rcf_source      FOREIGN KEY (source_code)      REFERENCES currencies(code),
    CONSTRAINT fk_rcf_destination FOREIGN KEY (destination_code) REFERENCES currencies(code),
    CONSTRAINT chk_rcf_factor_positive CHECK (factor > 0),
    CONSTRAINT uk_rcf_pair UNIQUE (source_code, destination_code)
);

CREATE INDEX idx_rcf_pair   ON rate_correction_factors (source_code, destination_code);
CREATE INDEX idx_rcf_active ON rate_correction_factors (active);

INSERT INTO rate_correction_factors (source_code, destination_code, factor, description) VALUES
    ('USD', 'EUR', 0.9980, 'USD→EUR spread correction'),
    ('EUR', 'USD', 0.9980, 'EUR→USD spread correction'),
    ('USD', 'RUB', 1.0020, 'USD→RUB spread correction'),
    ('RUB', 'USD', 0.9970, 'RUB→USD spread correction'),
    ('USD', 'GBP', 0.9975, 'USD→GBP spread correction'),
    ('GBP', 'USD', 0.9975, 'GBP→USD spread correction');