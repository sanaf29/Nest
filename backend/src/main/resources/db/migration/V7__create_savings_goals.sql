-- Sprint 2 / Increment 2: savings goals and contributions.
-- Contributions are tracked allocations (money earmarked for a goal), not money movements.
-- The saved amount is not stored: it is the sum of contributions.

CREATE TABLE savings_goals (
    id            BIGINT IDENTITY(1,1) NOT NULL,
    user_id       BIGINT               NOT NULL,
    name          NVARCHAR(100)        NOT NULL,
    currency      CHAR(3)              NOT NULL,
    target_amount DECIMAL(19,4)        NOT NULL,
    target_date   DATE                 NULL,
    status        VARCHAR(10)          NOT NULL,
    achieved_at   DATETIMEOFFSET(6)    NULL,
    version       BIGINT               NOT NULL CONSTRAINT DF_savings_goals_version DEFAULT 0,
    created_at    DATETIMEOFFSET(6)    NOT NULL,
    updated_at    DATETIMEOFFSET(6)    NOT NULL,
    CONSTRAINT PK_savings_goals PRIMARY KEY (id),
    CONSTRAINT FK_savings_goals_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT UQ_savings_goals_user_name UNIQUE (user_id, name),
    CONSTRAINT CK_savings_goals_currency CHECK (currency LIKE '[A-Z][A-Z][A-Z]'),
    CONSTRAINT CK_savings_goals_target CHECK (target_amount > 0),
    CONSTRAINT CK_savings_goals_status CHECK (status IN ('ACTIVE', 'ACHIEVED')),
    CONSTRAINT CK_savings_goals_achieved CHECK (
        (status = 'ACHIEVED' AND achieved_at IS NOT NULL) OR (status = 'ACTIVE' AND achieved_at IS NULL))
);

CREATE TABLE goal_contributions (
    id                BIGINT IDENTITY(1,1) NOT NULL,
    goal_id           BIGINT               NOT NULL,
    amount            DECIMAL(19,4)        NOT NULL,
    contribution_date DATE                 NOT NULL,
    note              NVARCHAR(255)        NULL,
    idempotency_key   VARCHAR(64)          NULL,
    created_at        DATETIMEOFFSET(6)    NOT NULL,
    updated_at        DATETIMEOFFSET(6)    NOT NULL,
    CONSTRAINT PK_goal_contributions PRIMARY KEY (id),
    CONSTRAINT FK_goal_contributions_goal FOREIGN KEY (goal_id) REFERENCES savings_goals (id) ON DELETE CASCADE,
    CONSTRAINT CK_goal_contributions_amount CHECK (amount > 0)
);

CREATE INDEX IX_goal_contributions_goal ON goal_contributions (goal_id);

-- A retried request with the same key cannot record the contribution twice.
CREATE UNIQUE INDEX UX_goal_contributions_idempotency
    ON goal_contributions (goal_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
