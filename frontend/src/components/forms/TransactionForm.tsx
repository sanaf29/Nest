import { useMemo, useState, type FormEvent } from 'react';
import { Button } from '../ui/Button';
import { SelectField, TextField } from '../ui/Field';
import { FormError } from '../ui/States';
import { useFormSubmit } from '../../hooks/useFormSubmit';
import { transactionService } from '../../services/transactionService';
import type { AccountResponse, CategoryResponse, TransactionResponse } from '../../types/api';
import type { FieldErrors } from '../../utils/errors';
import { todayIso } from '../../utils/dates';
import { isValidAmount } from '../../utils/money';
import { cn } from '../../utils/cn';

interface TransactionFormProps {
  accounts: AccountResponse[];
  categories: CategoryResponse[];
  transaction?: TransactionResponse;
  defaultAccountId?: number;
  onSaved: () => void;
  onCancel: () => void;
}

type EntryType = 'EXPENSE' | 'INCOME';

/** Records or edits an income or expense. Only categories matching the chosen type are offered. */
export function TransactionForm({ accounts, categories, transaction, defaultAccountId, onSaved, onCancel }: TransactionFormProps) {
  const [type, setType] = useState<EntryType>(transaction?.type === 'INCOME' ? 'INCOME' : 'EXPENSE');
  const [accountId, setAccountId] = useState<string>(String(transaction?.accountId ?? defaultAccountId ?? accounts[0]?.id ?? ''));
  const [categoryId, setCategoryId] = useState<string>(String(transaction?.categoryId ?? ''));
  const [amount, setAmount] = useState(transaction ? String(transaction.amount) : '');
  const [date, setDate] = useState(transaction?.transactionDate ?? todayIso());
  const [description, setDescription] = useState(transaction?.description ?? '');
  const { submitting, formError, fieldErrors, submit } = useFormSubmit();

  const account = accounts.find((a) => String(a.id) === accountId);
  const currency = account?.currency ?? 'CAD';
  const options = useMemo(
    () => categories.filter((c) => c.categoryType === type && (!c.archived || String(c.id) === categoryId)),
    [categories, type, categoryId],
  );

  function chooseType(next: EntryType) {
    setType(next);
    if (!categories.some((c) => String(c.id) === categoryId && c.categoryType === next)) setCategoryId('');
  }

  function validate(): FieldErrors {
    const errors: FieldErrors = {};
    if (!account) errors.accountId = 'Choose an account.';
    if (!categoryId) errors.categoryId = 'Choose a category.';
    if (!isValidAmount(amount, currency) || Number(amount) <= 0) {
      errors.amount = `Enter a positive amount${currency === 'JPY' ? '' : ', e.g. 84.37'}.`;
    }
    if (!date) errors.transactionDate = 'Choose a date.';
    return errors;
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    await submit(validate(), async () => {
      const request = {
        accountId: Number(accountId),
        categoryId: Number(categoryId),
        type,
        amount: Number(amount),
        transactionDate: date,
        description: description.trim() || undefined,
      };
      if (transaction) await transactionService.update(transaction.id, request);
      else await transactionService.create(request);
      onSaved();
    });
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4">
      <FormError message={formError} />
      <div role="radiogroup" aria-label="Type" className="grid grid-cols-2 gap-1 rounded-lg bg-ink-100 p-1">
        {(['EXPENSE', 'INCOME'] as const).map((option) => (
          <button
            key={option}
            type="button"
            role="radio"
            aria-checked={type === option}
            onClick={() => chooseType(option)}
            className={cn('rounded-md py-1.5 text-sm font-medium', type === option ? 'bg-white text-ink-900 shadow-sm' : 'text-ink-600')}
          >
            {option === 'EXPENSE' ? 'Expense' : 'Income'}
          </button>
        ))}
      </div>
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <TextField label={`Amount (${currency})`} inputMode="decimal" value={amount} onChange={(e) => setAmount(e.target.value)} error={fieldErrors.amount} placeholder="0.00" />
        <TextField label="Date" type="date" value={date} onChange={(e) => setDate(e.target.value)} error={fieldErrors.transactionDate} />
      </div>
      <SelectField label="Account" value={accountId} onChange={(e) => setAccountId(e.target.value)} error={fieldErrors.accountId}>
        <option value="">Choose…</option>
        {accounts.map((a) => (
          <option key={a.id} value={a.id}>
            {a.name} ({a.currency})
          </option>
        ))}
      </SelectField>
      <SelectField label="Category" value={categoryId} onChange={(e) => setCategoryId(e.target.value)} error={fieldErrors.categoryId}>
        <option value="">Choose…</option>
        {options.map((c) => (
          <option key={c.id} value={c.id}>
            {c.name}
          </option>
        ))}
      </SelectField>
      <TextField label="Description (optional)" value={description} onChange={(e) => setDescription(e.target.value)} error={fieldErrors.description} maxLength={255} placeholder="Weekly groceries" />
      <div className="flex justify-end gap-2 pt-2">
        <Button variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" loading={submitting}>
          {transaction ? 'Save changes' : type === 'EXPENSE' ? 'Add expense' : 'Add income'}
        </Button>
      </div>
    </form>
  );
}
