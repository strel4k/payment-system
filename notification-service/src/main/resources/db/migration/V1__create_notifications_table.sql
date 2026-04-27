CREATE TABLE notifications
(
    uid         UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    created_at  TIMESTAMP    DEFAULT now() NOT NULL,
    modified_at TIMESTAMP,
    user_uid    UUID         NOT NULL,
    message     TEXT         NOT NULL,
    subject     VARCHAR(255) NOT NULL,
    created_by  VARCHAR(255) NOT NULL,
    recipient_email VARCHAR(255),
    status      VARCHAR(20)  NOT NULL DEFAULT 'NEW',

    CONSTRAINT chk_notifications_status CHECK (status IN ('NEW', 'COMPLETED'))
);

CREATE INDEX idx_notifications_user_uid ON notifications (user_uid);

CREATE INDEX idx_notifications_status ON notifications (status);

CREATE INDEX idx_notifications_created_at ON notifications (created_at);

CREATE INDEX idx_notifications_user_status ON notifications (user_uid, status);