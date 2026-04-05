CREATE TABLE payment_providers
(
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL,
    description VARCHAR(256)
);