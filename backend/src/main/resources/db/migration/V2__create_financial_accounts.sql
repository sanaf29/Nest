-- Sprint 1 / Increment 2: financial accounts.
-- No balance column: the balance is derived from opening_balance and the account's transactions.
-- Accounts are never deleted (they are deactivated), so the foreign key uses NO ACTION.

CREATE TABLE financial_accounts (
    id              BIGINT IDENTITY(1,1) NOT NULL,
    user_id         BIGINT               NOT NULL,
    name            NVARCHAR(100)        NOT NULL,
    account_type    VARCHAR(20)          NOT NULL,
    currency        CHAR(3)              NOT NULL,
    opening_balance DECIMAL(19,4)        NOT NULL,
    active          BIT                  NOT NULL CONSTRAINT DF_financial_accounts_active DEFAULT 1,
    version         BIGINT               NOT NULL CONSTRAINT DF_financial_accounts_version DEFAULT 0,
    created_at      DATETIMEOFFSET(6)    NOT NULL,
    updated_at      DATETIMEOFFSET(6)    NOT NULL,
    CONSTRAINT PK_financial_accounts PRIMARY KEY (id),
    CONSTRAINT FK_financial_accounts_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT CK_financial_accounts_type CHECK (account_type IN ('CHECKING', 'SAVINGS', 'CASH', 'CREDIT_CARD')),
    CONSTRAINT CK_financial_accounts_currency CHECK (currency LIKE '[A-Z][A-Z][A-Z]'),
    -- Mirrors AccountType.allowsNegativeOpeningBalance() as a second line of defence.
    CONSTRAINT CK_financial_accounts_opening_balance
        CHECK (opening_balance >= 0 OR account_type IN ('CHECKING', 'CREDIT_CARD'))
);

CREATE INDEX IX_financial_accounts_user ON financial_accounts (user_id);

-- A closed account named "Visa" must not block opening a new "Visa".
-- The default collation is case-insensitive, so "visa" and "Visa" collide.
CREATE UNIQUE INDEX UX_financial_accounts_user_name_active
    ON financial_accounts (user_id, name)
    WHERE active = 1;
