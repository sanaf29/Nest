import { useState, type FormEvent } from 'react';
import { Button } from '../ui/Button';
import { SelectField, TextField } from '../ui/Field';
import { Icon } from '../ui/Icon';
import { MoneyText } from '../ui/MoneyText';
import { FormError } from '../ui/States';
import { useFormSubmit } from '../../hooks/useFormSubmit';
import { sharedExpenseService } from '../../services/sharedExpenseService';
import type { SplitMethod } from '../../types/api';
import type { FieldErrors } from '../../utils/errors';
import { todayIso } from '../../utils/dates';
import { isValidAmount } from '../../utils/money';
import { previewSplit } from '../../utils/split';

interface Person {
  key: number;
  name: string;
  email: string;
  value: string;
}

let nextKey = 1;

export function SharedExpenseForm({ currency, myName, onSaved, onCancel }: { currency: string; myName: string; onSaved: () => void; onCancel: () => void }) {
  const [description, setDescription] = useState('');
  const [total, setTotal] = useState('');
  const [date, setDate] = useState(todayIso());
  const [method, setMethod] = useState<SplitMethod>('EQUAL');
  const [people, setPeople] = useState<Person[]>([
    { key: nextKey++, name: myName, email: '', value: '' },
    { key: nextKey++, name: '', email: '', value: '' },
  ]);
  const [payerKey, setPayerKey] = useState(people[0]!.key);
  const meKey = people[0]!.key;
  const { submitting, formError, fieldErrors, submit } = useFormSubmit();

  const totalNumber = isValidAmount(total, currency) ? Number(total) : 0;
  const preview = previewSplit(method, totalNumber, people.map((p) => Number(p.value) || 0), currency);
  const update = (key: number, patch: Partial<Person>) => setPeople((ps) => ps.map((p) => (p.key === key ? { ...p, ...patch } : p)));

  function validate(): FieldErrors {
    const e: FieldErrors = {};
    if (!description.trim()) e.description = 'What was it for?';
    if (!isValidAmount(total, currency) || Number(total) <= 0) e.totalAmount = 'Enter the total.';
    const names = new Set<string>();
    people.forEach((p, i) => {
      const n = p.name.trim().toLowerCase();
      if (!n) e[`participants[${i}].name`] = 'Name needed.';
      else if (names.has(n)) e[`participants[${i}].name`] = 'Already listed.';
      names.add(n);
    });
    if (preview.error) e.participants = preview.error;
    return e;
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    await submit(validate(), async () => {
      await sharedExpenseService.create({
        description: description.trim(),
        totalAmount: Number(total),
        currency,
        expenseDate: date,
        splitMethod: method,
        participants: people.map((p) => ({
          name: p.name.trim(),
          email: p.email.trim() || undefined,
          me: p.key === meKey,
          paid: p.key === payerKey,
          value: method === 'EQUAL' ? undefined : Number(p.value) || 0,
        })),
      });
      onSaved();
    });
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4">
      <p className="rounded-lg bg-brand-50 px-3 py-2 text-sm text-brand-800">Nest only keeps track of who owes what. No money is moved.</p>
      <FormError message={formError} />
      <TextField label="What for" value={description} onChange={(e) => setDescription(e.target.value)} error={fieldErrors.description} placeholder="Cabin weekend" maxLength={150} />
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <TextField label={`Total (${currency})`} inputMode="decimal" value={total} onChange={(e) => setTotal(e.target.value)} error={fieldErrors.totalAmount} />
        <TextField label="Date" type="date" value={date} onChange={(e) => setDate(e.target.value)} />
        <SelectField label="Split" value={method} onChange={(e) => setMethod(e.target.value as SplitMethod)}>
          <option value="EQUAL">Equally</option>
          <option value="CUSTOM">By amount</option>
          <option value="PERCENTAGE">By percentage</option>
        </SelectField>
      </div>
      <fieldset className="flex flex-col gap-3">
        <legend className="mb-1 text-sm font-medium text-ink-700">People</legend>
        {people.map((p, i) => (
          <div key={p.key} className="grid grid-cols-[1fr_auto] items-start gap-2 rounded-xl bg-ink-50 p-3 sm:grid-cols-[1fr_7rem_auto_auto]">
            <TextField label={i === 0 ? 'You' : `Person ${i + 1}`} value={p.name} onChange={(e) => update(p.key, { name: e.target.value })} error={fieldErrors[`participants[${i}].name`]} maxLength={100} />
            {method !== 'EQUAL' ? (
              <TextField label={method === 'CUSTOM' ? 'Amount' : '%'} inputMode="decimal" value={p.value} onChange={(e) => update(p.key, { value: e.target.value })} />
            ) : (
              <div className="hidden sm:block" />
            )}
            <label className="mt-7 flex items-center gap-1.5 text-sm text-ink-700">
              <input type="radio" name="payer" checked={payerKey === p.key} onChange={() => setPayerKey(p.key)} />
              Paid
            </label>
            <div className="mt-6 flex items-center gap-2 justify-self-end">
              <span className="text-sm font-medium tabular-nums" aria-label={`${p.name || `Person ${i + 1}`} share`}>
                <MoneyText amount={preview.shares[i] ?? 0} currency={currency} />
              </span>
              {i > 0 && people.length > 2 && (
                <button type="button" onClick={() => { setPeople((ps) => ps.filter((x) => x.key !== p.key)); if (payerKey === p.key) setPayerKey(meKey); }}
                  className="rounded-lg p-1.5 text-ink-500 hover:bg-ink-100" aria-label={`Remove ${p.name || `person ${i + 1}`}`}>
                  <Icon name="trash" className="h-4 w-4" />
                </button>
              )}
            </div>
          </div>
        ))}
        {(fieldErrors.participants || preview.error) && <p role="alert" className="text-xs text-negative-600">{fieldErrors.participants ?? preview.error}</p>}
        {people.length < 20 && (
          <Button variant="secondary" size="sm" className="self-start" icon={<Icon name="plus" className="h-4 w-4" />}
            onClick={() => setPeople((ps) => [...ps, { key: nextKey++, name: '', email: '', value: '' }])}>
            Add person
          </Button>
        )}
      </fieldset>
      <div className="flex justify-end gap-2 pt-2">
        <Button variant="secondary" onClick={onCancel}>Cancel</Button>
        <Button type="submit" loading={submitting}>Save split</Button>
      </div>
    </form>
  );
}
