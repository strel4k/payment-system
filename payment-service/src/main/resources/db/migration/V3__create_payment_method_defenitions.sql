CREATE TABLE payment_method_definitions
(
    id                    SERIAL PRIMARY KEY,
    payment_method_id     INTEGER REFERENCES payment_methods (id),
    currency_code         VARCHAR(3),
    country_alpha3_code   VARCHAR(3),
    is_all_currencies     BOOLEAN DEFAULT FALSE,
    is_all_countries      BOOLEAN DEFAULT FALSE,
    is_priority           BOOLEAN DEFAULT FALSE,
    is_active             BOOLEAN DEFAULT TRUE
);

CREATE INDEX idx_payment_method_definitions_currency ON payment_method_definitions (currency_code);
CREATE INDEX idx_payment_method_definitions_country ON payment_method_definitions (country_alpha3_code);
CREATE INDEX idx_payment_method_definitions_method ON payment_method_definitions (payment_method_id);