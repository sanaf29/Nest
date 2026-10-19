-- Sprint 2 / Increment 1: monthly budgets and category allocations.
-- The savings target is not stored: it is derived from planned_income and the strategy.

CREATE TABLE monthly_budgets (
    id               BIGINT IDENTITY(1,1) NOT NULL,
    user_id          BIGINT               NOT NULL,
    budget_year      INT                  NOT NULL,
    budget_month     INT                  NOT NULL,
    currency         CHAR(3)              NOT NULL,
    planned_income   DECIMAL(19,4)        NOT NULL,
    savings_strategy VARCHAR(20)          NOT NULL,
    savings_value    DECIMAL(19,4)        NOT NULL,
    version          BIGINT               NOT NULL CONSTRAINT DF_monthly_budgets_version DEFAULT 0,
    created_at       DATETIMEOFFSET(6)    NOT NULL,
    updated_at       DATETIMEOFFSET(6)    NOT NULL,
    CONSTRAINT PK_monthly_budgets PRIMARY KEY (id),
    CONSTRAINT FK_monthly_budgets_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT UQ_monthly_budgets_user_period UNIQUE (user_id, budget_year, budget_month, currency),
    CONSTRAINT CK_monthly_budgets_month CHECK (budget_month BETWEEN 1 AND 12),
    CONSTRAINT CK_monthly_budgets_year CHECK (budget_year BETWEEN 2000 AND 2100),
    CONSTRAINT CK_monthly_budgets_currency CHECK (currency LIKE '[A-Z][A-Z][A-Z]'),
    CONSTRAINT CK_monthly_budgets_income CHECK (planned_income >= 0),
    CONSTRAINT CK_monthly_budgets_strategy CHECK (savings_strategy IN ('PERCENTAGE', 'FIXED_AMOUNT')),
    CONSTRAINT CK_monthly_budgets_savings_value CHECK (
        savings_value >= 0 AND (savings_strategy <> 'PERCENTAGE' OR savings_value <= 100))
);

-- Allocations belong to their budget: deleting a budget deletes its allocations.
CREATE TABLE budget_allocations (
    id           BIGINT IDENTITY(1,1) NOT NULL,
    budget_id    BIGINT               NOT NULL,
    category_id  BIGINT               NOT NULL,
    limit_amount DECIMAL(19,4)        NOT NULL,
    created_at   DATETIMEOFFSET(6)    NOT NULL,
    updated_at   DATETIMEOFFSET(6)    NOT NULL,
    CONSTRAINT PK_budget_allocations PRIMARY KEY (id),
    CONSTRAINT FK_budget_allocations_budget FOREIGN KEY (budget_id) REFERENCES monthly_budgets (id) ON DELETE CASCADE,
    CONSTRAINT FK_budget_allocations_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT UQ_budget_allocations_category UNIQUE (budget_id, category_id),
    CONSTRAINT CK_budget_allocations_limit CHECK (limit_amount > 0)
);

CREATE INDEX IX_budget_allocations_category ON budget_allocations (category_id);
