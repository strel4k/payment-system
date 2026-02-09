
-- Addresses
CREATE TABLE IF NOT EXISTS person.addresses_aud (
    id UUID NOT NULL,
    created TIMESTAMP,
    updated TIMESTAMP,
    country_id INTEGER,
    address VARCHAR(128),
    zip_code VARCHAR(32),
    archived TIMESTAMP,
    city VARCHAR(32),
    state VARCHAR(32),

    rev INTEGER NOT NULL,
    revtype SMALLINT,
    PRIMARY KEY (id, rev)
    );

-- Users
CREATE TABLE IF NOT EXISTS person.users_aud (
    id UUID NOT NULL,
    secret_key VARCHAR(32),
    email VARCHAR(1024),
    created TIMESTAMP,
    updated TIMESTAMP,
    first_name VARCHAR(32),
    last_name VARCHAR(32),
    filled BOOLEAN,
    address_id UUID,

    rev INTEGER NOT NULL,
    revtype SMALLINT,
    PRIMARY KEY (id, rev)
    );

-- Individuals
CREATE TABLE IF NOT EXISTS person.individuals_aud (
    id UUID NOT NULL,
    user_id UUID,
    passport_number VARCHAR(32),
    phone_number VARCHAR(32),
    verified_at TIMESTAMP,
    archived_at TIMESTAMP,
    status VARCHAR(32),

    rev INTEGER NOT NULL,
    revtype SMALLINT,
    PRIMARY KEY (id, rev)
    );

-- Countries
CREATE TABLE IF NOT EXISTS person.countries_aud (
    id INTEGER NOT NULL,
    created TIMESTAMP,
    updated TIMESTAMP,
    name VARCHAR(32),
    alpha2 VARCHAR(2),
    alpha3 VARCHAR(3),
    status VARCHAR(32),

    rev INTEGER NOT NULL,
    revtype SMALLINT,
    PRIMARY KEY (id, rev)
    );

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_addresses_aud_rev'
    ) THEN
ALTER TABLE person.addresses_aud
    ADD CONSTRAINT fk_addresses_aud_rev
        FOREIGN KEY (rev) REFERENCES person.revinfo(rev);
END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_users_aud_rev'
    ) THEN
ALTER TABLE person.users_aud
    ADD CONSTRAINT fk_users_aud_rev
        FOREIGN KEY (rev) REFERENCES person.revinfo(rev);
END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_individuals_aud_rev'
    ) THEN
ALTER TABLE person.individuals_aud
    ADD CONSTRAINT fk_individuals_aud_rev
        FOREIGN KEY (rev) REFERENCES person.revinfo(rev);
END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_countries_aud_rev'
    ) THEN
ALTER TABLE person.countries_aud
    ADD CONSTRAINT fk_countries_aud_rev
        FOREIGN KEY (rev) REFERENCES person.revinfo(rev);
END IF;
END$$;