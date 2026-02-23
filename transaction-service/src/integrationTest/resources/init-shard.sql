CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS wallet_types (
    uid UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    modified_at TIMESTAMP,
    name VARCHAR(32) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    status VARCHAR(18) NOT NULL DEFAULT 'ACTIVE',
    archived_at TIMESTAMP,
    user_type VARCHAR(15),
    creator VARCHAR(255),
    modifier VARCHAR(255),
    CONSTRAINT uk_wallet_types_name UNIQUE (name)
    );

CREATE TABLE IF NOT EXISTS wallets (
    uid UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    modified_at TIMESTAMP,
    name VARCHAR(32) NOT NULL,
    wallet_type_uid UUID NOT NULL,
    user_uid UUID NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    balance DECIMAL(19, 4) NOT NULL DEFAULT 0.0,
    archived_at TIMESTAMP,
    CONSTRAINT chk_wallets_balance_non_negative CHECK (balance >= 0)
    );

CREATE TABLE IF NOT EXISTS transactions (
    uid UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    modified_at TIMESTAMP,
    user_uid UUID NOT NULL,
    wallet_uid UUID NOT NULL,
    amount DECIMAL(19, 4) NOT NULL,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    comment VARCHAR(256),
    fee DECIMAL(19, 4) DEFAULT 0.0,
    target_wallet_uid UUID,
    payment_method_id BIGINT,
    failure_reason VARCHAR(256),
    CONSTRAINT chk_transactions_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_transactions_fee_non_negative CHECK (fee >= 0)
    );

INSERT INTO wallet_types (uid, name, currency_code, status, user_type, creator) VALUES
     ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'USD Wallet', 'USD', 'ACTIVE', 'INDIVIDUAL', 'system'),
     ('b1ffcd00-ad1c-5f09-cc7e-7cc0ce491b22', 'EUR Wallet', 'EUR', 'ACTIVE', 'INDIVIDUAL', 'system')
    ON CONFLICT (name) DO NOTHING;