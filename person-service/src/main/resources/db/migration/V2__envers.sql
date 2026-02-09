CREATE TABLE IF NOT EXISTS person.revinfo (
                                              rev INTEGER NOT NULL PRIMARY KEY,
                                              revtstmp BIGINT
);

CREATE SEQUENCE IF NOT EXISTS person.revinfo_seq
    START WITH 1
    INCREMENT BY 50;

ALTER TABLE person.revinfo
    ALTER COLUMN rev SET DEFAULT nextval('person.revinfo_seq');

ALTER SEQUENCE person.revinfo_seq OWNED BY person.revinfo.rev;