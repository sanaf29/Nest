-- Sprint 1 / Increment 1: user accounts.
-- Email is stored lower-cased by the application, so the unique constraint is case-insensitive in practice.

CREATE TABLE users (
    id                 BIGINT IDENTITY(1,1) NOT NULL,
    first_name         NVARCHAR(100)        NOT NULL,
    last_name          NVARCHAR(100)        NOT NULL,
    email              NVARCHAR(254)        NOT NULL,
    password_hash      VARCHAR(100)         NOT NULL,
    preferred_currency CHAR(3)              NOT NULL,
    created_at         DATETIMEOFFSET(6)    NOT NULL,
    updated_at         DATETIMEOFFSET(6)    NOT NULL,
    CONSTRAINT PK_users PRIMARY KEY (id),
    CONSTRAINT UQ_users_email UNIQUE (email),
    CONSTRAINT CK_users_preferred_currency CHECK (preferred_currency LIKE '[A-Z][A-Z][A-Z]')
);
