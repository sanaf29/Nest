-- Sprint 3 / Increment 3: reminders and in-app notifications.

CREATE TABLE notifications (
    id         BIGINT IDENTITY(1,1) NOT NULL,
    user_id    BIGINT               NOT NULL,
    category   VARCHAR(20)          NOT NULL,
    title      NVARCHAR(150)        NOT NULL,
    message    NVARCHAR(500)        NOT NULL,
    link       VARCHAR(200)         NULL,
    dedup_key  VARCHAR(120)         NOT NULL,
    read_at    DATETIMEOFFSET(6)    NULL,
    created_at DATETIMEOFFSET(6)    NOT NULL,
    updated_at DATETIMEOFFSET(6)    NOT NULL,
    CONSTRAINT PK_notifications PRIMARY KEY (id),
    CONSTRAINT FK_notifications_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT CK_notifications_category CHECK (category IN ('REMINDER', 'BUDGET', 'RECURRING', 'GOAL', 'SYSTEM')),
    -- Every notification source uses a deterministic key, so reruns and restarts never notify twice.
    CONSTRAINT UQ_notifications_user_dedup UNIQUE (user_id, dedup_key)
);

CREATE INDEX IX_notifications_user_unread ON notifications (user_id, read_at, created_at);

CREATE TABLE reminders (
    id                BIGINT IDENTITY(1,1) NOT NULL,
    user_id           BIGINT               NOT NULL,
    reminder_type     VARCHAR(30)          NOT NULL,
    title             NVARCHAR(150)        NOT NULL,
    notes             NVARCHAR(500)        NULL,
    due_at            DATETIMEOFFSET(6)    NULL,
    offset_minutes    INT                  NOT NULL CONSTRAINT DF_reminders_offset DEFAULT 0,
    -- Derived, persisted, indexable: when to notify. Never written by the application.
    remind_at         AS DATEADD(MINUTE, -offset_minutes, due_at) PERSISTED,
    status            VARCHAR(10)          NOT NULL,
    sent_at           DATETIMEOFFSET(6)    NULL,
    location          NVARCHAR(200)        NULL,
    savings_goal_id   BIGINT               NULL,
    category_id       BIGINT               NULL,
    threshold_percent INT                  NULL,
    version           BIGINT               NOT NULL CONSTRAINT DF_reminders_version DEFAULT 0,
    created_at        DATETIMEOFFSET(6)    NOT NULL,
    updated_at        DATETIMEOFFSET(6)    NOT NULL,
    CONSTRAINT PK_reminders PRIMARY KEY (id),
    CONSTRAINT FK_reminders_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT FK_reminders_goal FOREIGN KEY (savings_goal_id) REFERENCES savings_goals (id) ON DELETE CASCADE,
    CONSTRAINT FK_reminders_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT CK_reminders_type CHECK (reminder_type IN
        ('BILL_PAYMENT', 'BUDGET_THRESHOLD', 'SUBSCRIPTION_RENEWAL', 'SAVINGS_GOAL_DEADLINE', 'PARKING', 'CUSTOM')),
    CONSTRAINT CK_reminders_status CHECK (status IN ('SCHEDULED', 'SENT', 'DISMISSED')),
    CONSTRAINT CK_reminders_offset CHECK (offset_minutes BETWEEN 0 AND 43200),
    -- Budget thresholds are evaluated continuously (no due date); every other type fires once at a time.
    CONSTRAINT CK_reminders_shape CHECK (
        (reminder_type = 'BUDGET_THRESHOLD' AND threshold_percent BETWEEN 1 AND 200 AND due_at IS NULL)
        OR (reminder_type <> 'BUDGET_THRESHOLD' AND due_at IS NOT NULL AND threshold_percent IS NULL)),
    CONSTRAINT CK_reminders_parking CHECK (reminder_type <> 'PARKING' OR location IS NOT NULL),
    CONSTRAINT CK_reminders_goal CHECK (reminder_type <> 'SAVINGS_GOAL_DEADLINE' OR savings_goal_id IS NOT NULL)
);

CREATE INDEX IX_reminders_due ON reminders (status, remind_at);
CREATE INDEX IX_reminders_user ON reminders (user_id);
