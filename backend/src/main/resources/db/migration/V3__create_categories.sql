-- Sprint 1 / Increment 3: income and expense categories.
-- user_id NULL = system default category, visible to everyone and editable by no one.
-- "Is default" is not stored: it is exactly "user_id IS NULL" (3NF).
-- Categories are archived, never deleted, so transactions keep their classification.

CREATE TABLE categories (
    id            BIGINT IDENTITY(1,1) NOT NULL,
    user_id       BIGINT               NULL,
    name          NVARCHAR(50)         NOT NULL,
    category_type VARCHAR(10)          NOT NULL,
    color         CHAR(7)              NOT NULL,
    archived      BIT                  NOT NULL CONSTRAINT DF_categories_archived DEFAULT 0,
    version       BIGINT               NOT NULL CONSTRAINT DF_categories_version DEFAULT 0,
    created_at    DATETIMEOFFSET(6)    NOT NULL,
    updated_at    DATETIMEOFFSET(6)    NOT NULL,
    CONSTRAINT PK_categories PRIMARY KEY (id),
    CONSTRAINT FK_categories_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT CK_categories_type CHECK (category_type IN ('INCOME', 'EXPENSE')),
    CONSTRAINT CK_categories_color CHECK (color LIKE '#[0-9A-F][0-9A-F][0-9A-F][0-9A-F][0-9A-F][0-9A-F]'),
    CONSTRAINT CK_categories_default_not_archived CHECK (user_id IS NOT NULL OR archived = 0)
);

CREATE INDEX IX_categories_user ON categories (user_id);

-- Unique active names per owner and type. The default collation is case-insensitive.
CREATE UNIQUE INDEX UX_categories_user_name_type
    ON categories (user_id, name, category_type)
    WHERE user_id IS NOT NULL AND archived = 0;

CREATE UNIQUE INDEX UX_categories_default_name_type
    ON categories (name, category_type)
    WHERE user_id IS NULL;

INSERT INTO categories (user_id, name, category_type, color, created_at, updated_at)
VALUES
    (NULL, N'Salary',         'INCOME',  '#16A34A', SYSDATETIMEOFFSET(), SYSDATETIMEOFFSET()),
    (NULL, N'Freelance',      'INCOME',  '#22C55E', SYSDATETIMEOFFSET(), SYSDATETIMEOFFSET()),
    (NULL, N'Investments',    'INCOME',  '#0D9488', SYSDATETIMEOFFSET(), SYSDATETIMEOFFSET()),
    (NULL, N'Gifts',          'INCOME',  '#84CC16', SYSDATETIMEOFFSET(), SYSDATETIMEOFFSET()),
    (NULL, N'Refunds',        'INCOME',  '#14B8A6', SYSDATETIMEOFFSET(), SYSDATETIMEOFFSET()),
    (NULL, N'Other Income',   'INCOME',  '#65A30D', SYSDATETIMEOFFSET(), SYSDATETIMEOFFSET()),
    (NULL, N'Housing',        'EXPENSE', '#DC2626', SYSDATETIMEOFFSET(), SYSDATETIMEOFFSET()),
    (NULL, N'Utilities',      'EXPENSE', '#EA580C', SYSDATETIMEOFFSET(), SYSDATETIMEOFFSET()),
    (NULL, N'Groceries',      'EXPENSE', '#D97706', SYSDATETIMEOFFSET(), SYSDATETIMEOFFSET()),
    (NULL, N'Dining Out',     'EXPENSE', '#F59E0B', SYSDATETIMEOFFSET(), SYSDATETIMEOFFSET()),
    (NULL, N'Transportation', 'EXPENSE', '#2563EB', SYSDATETIMEOFFSET(), SYSDATETIMEOFFSET()),
    (NULL, N'Car',            'EXPENSE', '#3B82F6', SYSDATETIMEOFFSET(), SYSDATETIMEOFFSET()),
    (NULL, N'Insurance',      'EXPENSE', '#4F46E5', SYSDATETIMEOFFSET(), SYSDATETIMEOFFSET()),
    (NULL, N'Healthcare',     'EXPENSE', '#E11D48', SYSDATETIMEOFFSET(), SYSDATETIMEOFFSET()),
    (NULL, N'Subscriptions',  'EXPENSE', '#7C3AED', SYSDATETIMEOFFSET(), SYSDATETIMEOFFSET()),
    (NULL, N'Entertainment',  'EXPENSE', '#C026D3', SYSDATETIMEOFFSET(), SYSDATETIMEOFFSET()),
    (NULL, N'Shopping',       'EXPENSE', '#DB2777', SYSDATETIMEOFFSET(), SYSDATETIMEOFFSET()),
    (NULL, N'Education',      'EXPENSE', '#0284C7', SYSDATETIMEOFFSET(), SYSDATETIMEOFFSET()),
    (NULL, N'Personal Care',  'EXPENSE', '#9333EA', SYSDATETIMEOFFSET(), SYSDATETIMEOFFSET()),
    (NULL, N'Travel',         'EXPENSE', '#0891B2', SYSDATETIMEOFFSET(), SYSDATETIMEOFFSET()),
    (NULL, N'Other Expenses', 'EXPENSE', '#6B7280', SYSDATETIMEOFFSET(), SYSDATETIMEOFFSET());
