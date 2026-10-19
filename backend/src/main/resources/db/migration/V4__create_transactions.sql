-- Sprint 1 / Increment 4: income and expense transactions.
-- Amounts are always positive; the type gives the direction.
-- No currency column: it is the account's currency (3NF; a mismatch is impossible).
-- Increment 5 extends this table for transfer legs.

CREATE TABLE transactions (
    id               BIGINT IDENTITY(1,1) NOT NULL,
    account_id       BIGINT               NOT NULL,
    category_id      BIGINT               NOT NULL,
    transaction_type VARCHAR(20)          NOT NULL,
    amount           DECIMAL(19,4)        NOT NULL,
    description      NVARCHAR(255)        NULL,
    transaction_date DATE                 NOT NULL,
    version          BIGINT               NOT NULL CONSTRAINT DF_transactions_version DEFAULT 0,
    created_at       DATETIMEOFFSET(6)    NOT NULL,
    updated_at       DATETIMEOFFSET(6)    NOT NULL,
    CONSTRAINT PK_transactions PRIMARY KEY (id),
    CONSTRAINT FK_transactions_account FOREIGN KEY (account_id) REFERENCES financial_accounts (id),
    CONSTRAINT FK_transactions_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT CK_transactions_type CHECK (transaction_type IN ('INCOME', 'EXPENSE')),
    CONSTRAINT CK_transactions_amount_positive CHECK (amount > 0)
);

-- Account history, monthly summaries, and balance aggregation.
CREATE INDEX IX_transactions_account_date ON transactions (account_id, transaction_date);
-- Category filters and spending by category.
CREATE INDEX IX_transactions_category ON transactions (category_id);
