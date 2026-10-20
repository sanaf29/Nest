-- Sprint 3 / Increment 2: recurring income and bills.
-- The owner is reached through the account (no user_id copy).
-- Generated occurrences are ordinary transactions stamped with (recurring_transaction_id, occurrence_date);
-- the unique index makes generation idempotent, so a rerun or a concurrent run cannot duplicate an occurrence.

CREATE TABLE recurring_transactions (
    id                 BIGINT IDENTITY(1,1) NOT NULL,
    account_id         BIGINT               NOT NULL,
    category_id        BIGINT               NOT NULL,
    transaction_type   VARCHAR(10)          NOT NULL,
    amount             DECIMAL(19,4)        NOT NULL,
    description        NVARCHAR(255)        NOT NULL,
    frequency          VARCHAR(12)          NOT NULL,
    start_date         DATE                 NOT NULL,
    end_date           DATE                 NULL,
    anchor_day         INT                  NOT NULL,
    next_occurrence    DATE                 NULL,
    status             VARCHAR(10)          NOT NULL,
    pause_reason       NVARCHAR(255)        NULL,
    remind_days_before INT                  NULL,
    version            BIGINT               NOT NULL CONSTRAINT DF_recurring_version DEFAULT 0,
    created_at         DATETIMEOFFSET(6)    NOT NULL,
    updated_at         DATETIMEOFFSET(6)    NOT NULL,
    CONSTRAINT PK_recurring_transactions PRIMARY KEY (id),
    CONSTRAINT FK_recurring_account FOREIGN KEY (account_id) REFERENCES financial_accounts (id),
    CONSTRAINT FK_recurring_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT CK_recurring_type CHECK (transaction_type IN ('INCOME', 'EXPENSE')),
    CONSTRAINT CK_recurring_amount CHECK (amount > 0),
    CONSTRAINT CK_recurring_frequency CHECK (frequency IN ('WEEKLY', 'BIWEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY')),
    CONSTRAINT CK_recurring_status CHECK (status IN ('ACTIVE', 'PAUSED', 'ENDED')),
    CONSTRAINT CK_recurring_dates CHECK (end_date IS NULL OR end_date >= start_date),
    CONSTRAINT CK_recurring_anchor CHECK (anchor_day BETWEEN 1 AND 31),
    CONSTRAINT CK_recurring_remind CHECK (remind_days_before IS NULL OR remind_days_before BETWEEN 0 AND 30)
);

-- The scheduler's query: active rules that are due.
CREATE INDEX IX_recurring_due ON recurring_transactions (status, next_occurrence);
CREATE INDEX IX_recurring_account ON recurring_transactions (account_id);

ALTER TABLE transactions ADD recurring_transaction_id BIGINT NULL, occurrence_date DATE NULL;
GO

ALTER TABLE transactions ADD CONSTRAINT FK_transactions_recurring
    FOREIGN KEY (recurring_transaction_id) REFERENCES recurring_transactions (id);
ALTER TABLE transactions ADD CONSTRAINT CK_transactions_recurring CHECK (
    (recurring_transaction_id IS NULL AND occurrence_date IS NULL)
    OR (recurring_transaction_id IS NOT NULL AND occurrence_date IS NOT NULL));

CREATE UNIQUE INDEX UX_transactions_recurring_occurrence
    ON transactions (recurring_transaction_id, occurrence_date)
    WHERE recurring_transaction_id IS NOT NULL;
