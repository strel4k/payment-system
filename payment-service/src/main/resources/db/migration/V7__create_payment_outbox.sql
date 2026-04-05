CREATE TABLE payment_outbox
(
    id                       SERIAL PRIMARY KEY,
    payment_id               INTEGER NOT NULL REFERENCES payments (id),
    status                   VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    method_type              VARCHAR(32) NOT NULL,
    amount                   NUMERIC(18, 2) NOT NULL,
    currency                 VARCHAR(3) NOT NULL,
    attempts                 INTEGER NOT NULL DEFAULT 0,
    max_attempts             INTEGER NOT NULL DEFAULT 3,
    last_error               TEXT,
    created_at               TIMESTAMP DEFAULT NOW(),
    processed_at             TIMESTAMP
);

CREATE INDEX idx_payment_outbox_status ON payment_outbox (status);
CREATE INDEX idx_payment_outbox_payment_id ON payment_outbox (payment_id);