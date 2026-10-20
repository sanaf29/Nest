import { useState, type FormEvent } from 'react';
import { Button } from '../ui/Button';
import { SelectField, TextField } from '../ui/Field';
import { FormError } from '../ui/States';
import { useFormSubmit } from '../../hooks/useFormSubmit';
import { accountService } from '../../services/accountService';
import type { AccountResponse, AccountType } from '../../types/api';
import type { FieldErrors } from '../../utils/errors';
import { COMMON_CURRENCIES, isValidAmount } from '../../utils/money';

export const accountTypeLabels: Record<AccountType, string> = {
  CHECKING: 'Chequing',
  SAVINGS: 'Savings',
  CASH: 'Cash',
  CREDIT_CARD: 'Credit card',
};

const allowsNegative = (type: AccountType) => type === 'CHECKING' || type === 'CREDIT_CARD';

interface AccountFormProps {
  account?: AccountResponse;
  defaultCurrency: string;
  onSaved: (account: AccountResponse) => void;
  onCancel: () => void;
}

/** Creates an account, or edits the name and opening balance of an existing one (type and currency are fixed). */
export function AccountForm({ account, defaultCurrency, onSaved, onCancel }: AccountFormProps) {
  const [name, setName] = useState(account?.name ?? '');
  const [type, setType] = useState<AccountType>(account?.accountType ?? 'CHECKING');
  const [currency, setCurrency] = useState(account?.currency ?? defaultCurrency);
  const [opening, setOpening] = useState(account ? String(account.openingBalance) : '0');
  const { submitting, formError, fieldErrors, submit } = useFormSubmit();

  function validate(): FieldErrors {
    const errors: FieldErrors = {};
    if (!name.trim()) errors.name = 'Give the account a name.';
    if (!isValidAmount(opening, currency, { allowNegative: true })) {
      errors.openingBalance = 'Enter an amount, e.g. 1500.00.';
    } else if (Number(opening) < 0 && !allowsNegative(type)) {
      errors.openingBalance = `A ${accountTypeLabels[type].toLowerCase()} account cannot start below zero.`;
    }
    return errors;
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    await submit(validate(), async () => {
      const saved = account
        ? await accountService.update(account.id, { name: name.trim(), openingBalance: Number(opening) })
        : await accountService.create({ name: name.trim(), accountType: type, currency, openingBalance: Number(opening) });
      onSaved(saved);
    });
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4">
      <FormError message={formError} />
      <TextField label="Name" value={name} onChange={(e) => setName(e.target.value)} error={fieldErrors.name} placeholder="Everyday chequing" maxLength={100} />
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <SelectField label="Type" value={type} onChange={(e) => setType(e.target.value as AccountType)} disabled={!!account} hint={account ? 'Fixed once created' : undefined}>
          {Object.entries(accountTypeLabels).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </SelectField>
        <SelectField label="Currency" value={currency} onChange={(e) => setCurrency(e.target.value)} disabled={!!account} hint={account ? 'Fixed once created' : undefined}>
          {[...new Set([currency, ...COMMON_CURRENCIES])].map((c) => (
            <option key={c}>{c}</option>
          ))}
        </SelectField>
      </div>
      <TextField
        label="Opening balance"
        inputMode="decimal"
        value={opening}
        onChange={(e) => setOpening(e.target.value)}
        error={fieldErrors.openingBalance}
        hint={type === 'CREDIT_CARD' ? 'Enter money owed as a negative number, e.g. -250.00' : 'The balance when you started tracking.'}
      />
      <div className="flex justify-end gap-2 pt-2">
        <Button variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" loading={submitting}>
          {account ? 'Save changes' : 'Add account'}
        </Button>
      </div>
    </form>
  );
}
