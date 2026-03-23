CREATE TABLE payment_methods
(
    id                   SERIAL PRIMARY KEY,
    provider_id          INTEGER REFERENCES payment_providers (id),
    type                 VARCHAR(32)  NOT NULL,
    created_at           TIMESTAMP    DEFAULT NOW(),
    modified_at          TIMESTAMP,
    name                 VARCHAR(64)  NOT NULL,
    is_active            BOOLEAN      DEFAULT TRUE,
    provider_unique_id   VARCHAR(128) NOT NULL,
    provider_method_type VARCHAR(32)  NOT NULL,
    logo                 TEXT,
    profile_type         VARCHAR(24)  DEFAULT 'INDIVIDUAL' NOT NULL
);