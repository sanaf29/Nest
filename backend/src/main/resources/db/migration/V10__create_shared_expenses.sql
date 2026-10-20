-- Sprint 3 / Increment 4: shared expenses. Tracking only: no payments, no access to other users' accounts.
-- Each participant has exactly one share, so participant and share live in one table.

CREATE TABLE shared_expenses (
    id           BIGINT IDENTITY(1,1) NOT NULL,
    user_id      BIGINT               NOT NULL,
    description  NVARCHAR(150)        NOT NULL,
    total_amount DECIMAL(19,4)        NOT NULL,
    currency     CHAR(3)              NOT NULL,
    expense_date DATE                 NOT NULL,
    split_method VARCHAR(12)          NOT NULL,
    version      BIGINT               NOT NULL CONSTRAINT DF_shared_expenses_version DEFAULT 0,
    created_at   DATETIMEOFFSET(6)    NOT NULL,
    updated_at   DATETIMEOFFSET(6)    NOT NULL,
    CONSTRAINT PK_shared_expenses PRIMARY KEY (id),
    CONSTRAINT FK_shared_expenses_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT CK_shared_expenses_total CHECK (total_amount > 0),
    CONSTRAINT CK_shared_expenses_currency CHECK (currency LIKE '[A-Z][A-Z][A-Z]'),
    CONSTRAINT CK_shared_expenses_method CHECK (split_method IN ('EQUAL', 'CUSTOM', 'PERCENTAGE'))
);

CREATE INDEX IX_shared_expenses_user ON shared_expenses (user_id, expense_date);

CREATE TABLE expense_participants (
    id                BIGINT IDENTITY(1,1) NOT NULL,
    shared_expense_id BIGINT               NOT NULL,
    position          INT                  NOT NULL,
    name              NVARCHAR(100)        NOT NULL,
    email             NVARCHAR(254)        NULL,
    is_me             BIT                  NOT NULL,
    is_payer          BIT                  NOT NULL,
    share_amount      DECIMAL(19,4)        NOT NULL,
    settled_at        DATETIMEOFFSET(6)    NULL,
    created_at        DATETIMEOFFSET(6)    NOT NULL,
    updated_at        DATETIMEOFFSET(6)    NOT NULL,
    CONSTRAINT PK_expense_participants PRIMARY KEY (id),
    CONSTRAINT FK_expense_participants_expense FOREIGN KEY (shared_expense_id) REFERENCES shared_expenses (id) ON DELETE CASCADE,
    CONSTRAINT UQ_expense_participants_name UNIQUE (shared_expense_id, name),
    CONSTRAINT CK_expense_participants_share CHECK (share_amount >= 0),
    -- The payer owes nothing to themselves, so their share can never be "settled".
    CONSTRAINT CK_expense_participants_payer_settled CHECK (is_payer = 0 OR settled_at IS NULL)
);

-- Exactly one payer and one "me" per expense (presence is checked by the application).
CREATE UNIQUE INDEX UX_expense_participants_payer ON expense_participants (shared_expense_id) WHERE is_payer = 1;
CREATE UNIQUE INDEX UX_expense_participants_me ON expense_participants (shared_expense_id) WHERE is_me = 1;
