CREATE TABLE webhooks
(
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    payload TEXT,
    received_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notification_url VARCHAR(2048)
);

CREATE INDEX idx_webhooks_entity ON webhooks (event_type, entity_id);