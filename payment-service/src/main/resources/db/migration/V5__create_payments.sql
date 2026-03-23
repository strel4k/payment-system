CREATE TABLE payments
(
    id                      SERIAL PRIMARY KEY,
    payment_method_id       INTEGER REFERENCES payment_methods (id),
    internal_transaction_id VARCHAR(128),
    external_transaction_id VARCHAR(128),
    amount                  NUMERIC(18, 2) NOT NULL,
    currency                VARCHAR(3)     NOT NULL,
    status                  VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    created_at              TIMESTAMP               DEFAULT NOW(),
    modified_at             TIMESTAMP               DEFAULT NOW()
);

CREATE INDEX idx_payments_internal_transaction ON payments (internal_transaction_id);
CREATE INDEX idx_payments_status ON payments (status);