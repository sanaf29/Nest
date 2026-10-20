import { useState, type FormEvent } from 'react';
import { Button } from '../ui/Button';
import { SelectField, TextField } from '../ui/Field';
import { FormError } from '../ui/States';
import { useFormSubmit } from '../../hooks/useFormSubmit';
import { transferService } from '../../services/transactionService';
import type { AccountResponse } from '../../types/api';
import type { FieldErrors } from '../../utils/errors';
import { todayIso } from '../../utils/dates';
import { isValidAmount } from '../../utils/money';

/** Moves money between two of the user's accounts in the same currency. */
export function TransferForm({ accounts, onSaved, onCancel }: { accounts: AccountResponse[]; onSaved: () => void; onCancel: () => void }) {
  const [sourceId, setSourceId] = useState(String(accounts[0]?.id ?? ''));
  const [destinationId, setDestinationId] = useState(String(accounts[1]?.id ?? ''));
  const [amount, setAmount] = useState('');
  const [date, setDate] = useState(todayIso());
  const [description, setDescription] = useState('');
  const { submitting, formError, fieldErrors, submit } = useFormSubmit();

  const source = accounts.find((a) => String(a.id) === sourceId);
  const destination = accounts.find((a) => String(a.id) === destinationId);
  const currency = source?.currency ?? 'CAD';

  function validate(): FieldErrors {
    const errors: FieldErrors = {};
    if (!source) errors.sourceAccountId = 'Choose where the money comes from.';
    if (!destination) errors.destinationAccountId = 'Choose where the money goes.';
    else if (sourceId === destinationId) errors.destinationAccountId = 'Choose a different account.';
    else if (source && destination.currency !== source.currency) {
      errors.destinationAccountId = `Both accounts must be in ${source.currency}; currency conversion is not supported.`;
    }
    if (!isValidAmount(amount, currency) || Number(amount) <= 0) errors.amount = 'Enter a positive amount.';
    return errors;
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    await submit(validate(), async () => {
      await transferService.create({
        sourceAccountId: Number(sourceId),
        destinationAccountId: Number(destinationId),
        amount: Number(amount),
        transferDate: date,
        description: description.trim() || undefined,
      });
      onSaved();
    });
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4">
      <p className="rounded-lg bg-brand-50 px-3 py-2 text-sm text-brand-800">
        Transfers move money between your accounts. They are not counted as income or spending. Paying a credit card is a transfer from chequing to the card.
      </p>
      <FormError message={formError} />
      <SelectField label="From" value={sourceId} onChange={(e) => setSourceId(e.target.value)} error={fieldErrors.sourceAccountId}>
        <option value="">Choose…</option>
        {accounts.map((a) => (
          <option key={a.id} value={a.id}>
            {a.name} ({a.currency})
          </option>
        ))}
      </SelectField>
      <SelectField label="To" value={destinationId} onChange={(e) => setDestinationId(e.target.value)} error={fieldErrors.destinationAccountId}>
        <option value="">Choose…</option>
        {accounts.map((a) => (
          <option key={a.id} value={a.id}>
            {a.name} ({a.currency})
          </option>
        ))}
      </SelectField>
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <TextField label={`Amount (${currency})`} inputMode="decimal" value={amount} onChange={(e) => setAmount(e.target.value)} error={fieldErrors.amount} />
        <TextField label="Date" type="date" value={date} onChange={(e) => setDate(e.target.value)} />
      </div>
      <TextField label="Description (optional)" value={description} onChange={(e) => setDescription(e.target.value)} maxLength={255} placeholder="Monthly savings" />
      <div className="flex justify-end gap-2 pt-2">
        <Button variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" loading={submitting}>
          Transfer
        </Button>
      </div>
    </form>
  );
}
