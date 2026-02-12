
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TYPE wallet_status AS ENUM ('ACTIVE', 'BLOCKED', 'CLOSED');
CREATE TYPE payment_type AS ENUM ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER');
CREATE TYPE transaction_status AS ENUM ('PENDING', 'COMPLETED', 'FAILED');

CREATE TABLE wallet_types (
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

CREATE INDEX idx_wallet_types_currency ON wallet_types(currency_code);

CREATE TABLE wallets (
    uid UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    modified_at TIMESTAMP,
    name VARCHAR(32) NOT NULL,
    wallet_type_uid UUID NOT NULL,
    user_uid UUID NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    balance DECIMAL(19, 4) NOT NULL DEFAULT 0.0,
    archived_at TIMESTAMP,

    CONSTRAINT fk_wallets_wallet_type
        FOREIGN KEY (wallet_type_uid) REFERENCES wallet_types(uid),
    CONSTRAINT chk_wallets_balance_non_negative
        CHECK (balance >= 0)
);

CREATE INDEX idx_wallets_user_uid ON wallets(user_uid);
CREATE INDEX idx_wallets_wallet_type_uid ON wallets(wallet_type_uid);
CREATE INDEX idx_wallets_status ON wallets(status);

CREATE TABLE transactions (
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

    CONSTRAINT fk_transactions_wallet
        FOREIGN KEY (wallet_uid) REFERENCES wallets(uid),
    CONSTRAINT fk_transactions_target_wallet
        FOREIGN KEY (target_wallet_uid) REFERENCES wallets(uid),
    CONSTRAINT chk_transactions_amount_positive
        CHECK (amount > 0),
    CONSTRAINT chk_transactions_fee_non_negative
        CHECK (fee >= 0)
);

CREATE INDEX idx_transactions_user_uid ON transactions(user_uid);
CREATE INDEX idx_transactions_wallet_uid ON transactions(wallet_uid);
CREATE INDEX idx_transactions_target_wallet_uid ON transactions(target_wallet_uid);
CREATE INDEX idx_transactions_type ON transactions(type);
CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);

CREATE INDEX idx_transactions_user_type_status ON transactions(user_uid, type, status);

INSERT INTO wallet_types (uid, name, currency_code, status, user_type, creator) VALUES
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'USD Wallet', 'USD', 'ACTIVE', 'INDIVIDUAL', 'system'),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'EUR Wallet', 'EUR', 'ACTIVE', 'INDIVIDUAL', 'system'),
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', 'RUB Wallet', 'RUB', 'ACTIVE', 'INDIVIDUAL', 'system');