CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE payment_provider_callbacks
(
    uid UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    created_at TIMESTAMP DEFAULT now() NOT NULL,
    updated_at TIMESTAMP DEFAULT now() NOT NULL,
    body TEXT NOT NULL,
    provider_transaction_uid UUID,
    type VARCHAR(255),
    provider VARCHAR(255)
);

CREATE TABLE verification_callbacks
(
    uid UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    created_at TIMESTAMP DEFAULT now() NOT NULL,
    modified_at TIMESTAMP DEFAULT now() NOT NULL,
    body TEXT NOT NULL,
    transaction_uid UUID,
    profile_uid UUID NOT NULL,
    status VARCHAR(25),
    type VARCHAR(255)
);

CREATE TABLE unknown_callbacks
(
    uid UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    created_at TIMESTAMP DEFAULT now() NOT NULL,
    updated_at TIMESTAMP DEFAULT now() NOT NULL,
    body TEXT NOT NULL
);