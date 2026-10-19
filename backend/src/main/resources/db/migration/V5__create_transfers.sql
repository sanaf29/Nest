-- Sprint 1 / Increment 5: transfers between a user's own accounts.
-- A transfer is one row in `transfers` plus two linked entries in `transactions`:
-- TRANSFER_OUT on the source account and TRANSFER_IN on the destination account.
-- Transfer entries have no category: moving your own money is neither income nor spending.

CREATE TABLE transfers (
    id         BIGINT IDENTITY(1,1) NOT NULL,
    created_at DATETIMEOFFSET(6)    NOT NULL,
    updated_at DATETIMEOFFSET(6)    NOT NULL,
    CONSTRAINT PK_transfers PRIMARY KEY (id)
);

-- category_id becomes optional (transfer entries have none). SQL Server will not change the
-- nullability of a column used by a foreign key or index, so both are dropped and recreated.
ALTER TABLE transactions DROP CONSTRAINT FK_transactions_category;
DROP INDEX IX_transactions_category ON transactions;
ALTER TABLE transactions ALTER COLUMN category_id BIGINT NULL;
ALTER TABLE transactions ADD CONSTRAINT FK_transactions_category FOREIGN KEY (category_id) REFERENCES categories (id);
CREATE INDEX IX_transactions_category ON transactions (category_id);

ALTER TABLE transactions ADD transfer_id BIGINT NULL;
GO

ALTER TABLE transactions ADD CONSTRAINT FK_transactions_transfer FOREIGN KEY (transfer_id) REFERENCES transfers (id);

ALTER TABLE transactions DROP CONSTRAINT CK_transactions_type;
ALTER TABLE transactions ADD CONSTRAINT CK_transactions_type
    CHECK (transaction_type IN ('INCOME', 'EXPENSE', 'TRANSFER_IN', 'TRANSFER_OUT'));

-- Income/expense: category required, no transfer. Transfer entry: transfer required, no category.
ALTER TABLE transactions ADD CONSTRAINT CK_transactions_shape CHECK (
    (transaction_type IN ('INCOME', 'EXPENSE') AND category_id IS NOT NULL AND transfer_id IS NULL)
    OR (transaction_type IN ('TRANSFER_IN', 'TRANSFER_OUT') AND category_id IS NULL AND transfer_id IS NOT NULL)
);

-- At most one outgoing and one incoming entry per transfer. Also serves lookups by transfer.
CREATE UNIQUE INDEX UX_transactions_transfer_type
    ON transactions (transfer_id, transaction_type)
    WHERE transfer_id IS NOT NULL;
