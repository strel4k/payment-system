CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE currencies (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(10) NOT NULL,
    iso_code VARCHAR(10) NOT NULL,
    description VARCHAR(255) NOT NULL,
    symbol VARCHAR(10),
    active BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT uk_currencies_code UNIQUE (code),
    CONSTRAINT uk_currencies_iso_code UNIQUE (iso_code)
);

CREATE INDEX idx_currencies_active ON currencies (active);

CREATE TABLE rate_providers (
    provider_code VARCHAR(20) NOT NULL,
    provider_name VARCHAR(100) NOT NULL,
    priority INT NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_rate_providers PRIMARY KEY (provider_code)
);

CREATE TABLE conversion_rates (
    id BIGSERIAL PRIMARY KEY,
    source_code VARCHAR(10) NOT NULL,
    destination_code VARCHAR(10) NOT NULL,
    rate DECIMAL(20, 8) NOT NULL,
    rate_begin_time TIMESTAMP NOT NULL,
    rate_end_time TIMESTAMP NOT NULL,
    provider_code VARCHAR(20) NOT NULL,

    CONSTRAINT fk_cr_source FOREIGN KEY (source_code) REFERENCES currencies (code),
    CONSTRAINT fk_cr_destination FOREIGN KEY (destination_code) REFERENCES currencies (code),
    CONSTRAINT fk_cr_provider FOREIGN KEY (provider_code) REFERENCES rate_providers (provider_code),
    CONSTRAINT chk_cr_rate_positive CHECK (rate > 0),
    CONSTRAINT chk_cr_time_order CHECK (rate_end_time > rate_begin_time)
);

CREATE INDEX idx_cr_lookup ON conversion_rates (source_code, destination_code, rate_begin_time, rate_end_time);
CREATE INDEX idx_cr_end_time ON conversion_rates (rate_end_time);

CREATE TABLE shedlock (
    name VARCHAR(64) NOT NULL,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL,

    CONSTRAINT pk_shedlock PRIMARY KEY (name)
);

INSERT INTO rate_providers (provider_code, provider_name, priority, active) VALUES
    ('EXR', 'ExchangeRate-API (exchangerate-api.com)', 1, TRUE);

INSERT INTO currencies (code, iso_code, description, symbol, active) VALUES
    ('USD', 'USD', 'US Dollar', '$', TRUE),
    ('EUR', 'EUR', 'Euro', '€', TRUE),
    ('RUB', 'RUB', 'Russian Ruble', '₽', TRUE),
    ('GBP', 'GBP', 'British Pound', '£', TRUE),
    ('CNY', 'CNY', 'Chinese Yuan', '¥', TRUE),
    ('JPY', 'JPY', 'Japanese Yen', '¥', TRUE),
    ('CHF', 'CHF', 'Swiss Franc', 'Fr', TRUE),
    ('CAD', 'CAD', 'Canadian Dollar', 'C$', TRUE),
    ('AUD', 'AUD', 'Australian Dollar', 'A$', TRUE),
    ('TRY', 'TRY', 'Turkish Lira', '₺', TRUE);