import { useState, type FormEvent } from 'react';
import { Button } from '../ui/Button';
import { SelectField, TextField } from '../ui/Field';
import { FormError } from '../ui/States';
import { useFormSubmit } from '../../hooks/useFormSubmit';
import { recurringService } from '../../services/recurringService';
import type { AccountResponse, CategoryResponse, Frequency, RecurringResponse } from '../../types/api';
import type { FieldErrors } from '../../utils/errors';
import { todayIso } from '../../utils/dates';
import { isValidAmount } from '../../utils/money';

export const frequencyLabels: Record<Frequency, string> = {
  WEEKLY: 'Weekly',
  BIWEEKLY: 'Every 2 weeks',
  MONTHLY: 'Monthly',
  QUARTERLY: 'Every 3 months',
  YEARLY: 'Yearly',
};

interface Props {
  accounts: AccountResponse[];
  categories: CategoryResponse[];
  rule?: RecurringResponse;
  onSaved: () => void;
  onCancel: () => void;
}

/** Schedule or edit recurring income or a bill. Type, frequency, and start date are fixed after creation. */
export function RecurringForm({ accounts, categories, rule, onSaved, onCancel }: Props) {
  const [type, setType] = useState<'INCOME' | 'EXPENSE'>(rule?.type ?? 'EXPENSE');
  const [description, setDescription] = useState(rule?.description ?? '');
  const [amount, setAmount] = useState(rule ? String(rule.amount) : '');
  const [accountId, setAccountId] = useState(String(rule?.accountId ?? accounts[0]?.id ?? ''));
  const [categoryId, setCategoryId] = useState(String(rule?.categoryId ?? ''));
  const [frequency, setFrequency] = useState<Frequency>(rule?.frequency ?? 'MONTHLY');
  const [startDate, setStartDate] = useState(rule?.startDate ?? todayIso());
  const [endDate, setEndDate] = useState(rule?.endDate ?? '');
  const [remind, setRemind] = useState(rule?.remindDaysBefore != null ? String(rule.remindDaysBefore) : '');
  const { submitting, formError, fieldErrors, submit } = useFormSubmit();

  const currency = accounts.find((a) => String(a.id) === accountId)?.currency ?? 'CAD';
  const options = categories.filter((c) => c.categoryType === type && (!c.archived || String(c.id) === categoryId));

  function validate(): FieldErrors {
    const e: FieldErrors = {};
    if (!description.trim()) e.description = 'Name it, e.g. Rent or Netflix.';
    if (!isValidAmount(amount, currency) || Number(amount) <= 0) e.amount = 'Enter a positive amount.';
    if (!accountId) e.accountId = 'Choose an account.';
    if (!categoryId) e.categoryId = 'Choose a category.';
    if (endDate && endDate < startDate) e.endDate = 'End date must be after the start date.';
    return e;
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    await submit(validate(), async () => {
      const common = {
        accountId: Number(accountId),
        categoryId: Number(categoryId),
        amount: Number(amount),
        description: description.trim(),
        endDate: endDate || null,
        remindDaysBefore: remind === '' ? null : Number(remind),
      };
      if (rule) await recurringService.update(rule.id, common);
      else await recurringService.create({ ...common, type, frequency, startDate });
      onSaved();
    });
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4">
      <FormError message={formError} />
      {!rule && (
        <SelectField label="Type" value={type} onChange={(e) => { setType(e.target.value as 'INCOME' | 'EXPENSE'); setCategoryId(''); }}>
          <option value="EXPENSE">Bill or expense</option>
          <option value="INCOME">Income</option>
        </SelectField>
      )}
      <TextField label="Description" value={description} onChange={(e) => setDescription(e.target.value)} error={fieldErrors.description} placeholder="Rent" maxLength={255} />
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <TextField label={`Amount (${currency})`} inputMode="decimal" value={amount} onChange={(e) => setAmount(e.target.value)} error={fieldErrors.amount} />
        <SelectField label="Repeats" value={frequency} onChange={(e) => setFrequency(e.target.value as Frequency)} disabled={!!rule}>
          {Object.entries(frequencyLabels).map(([value, label]) => (
            <option key={value} value={value}>{label}</option>
          ))}
        </SelectField>
      </div>
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <SelectField label="Account" value={accountId} onChange={(e) => setAccountId(e.target.value)} error={fieldErrors.accountId}>
          {accounts.map((a) => (
            <option key={a.id} value={a.id}>{a.name} ({a.currency})</option>
          ))}
        </SelectField>
        <SelectField label="Category" value={categoryId} onChange={(e) => setCategoryId(e.target.value)} error={fieldErrors.categoryId}>
          <option value="">Choose…</option>
          {options.map((c) => (
            <option key={c.id} value={c.id}>{c.name}</option>
          ))}
        </SelectField>
      </div>
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <TextField label="First date" type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} disabled={!!rule}
          hint={!rule && startDate <= todayIso() ? 'Past and today’s occurrences are recorded right away.' : undefined} />
        <TextField label="End date (optional)" type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} error={fieldErrors.endDate} />
        <SelectField label="Remind me" value={remind} onChange={(e) => setRemind(e.target.value)}>
          <option value="">No reminder</option>
          <option value="0">On the day</option>
          <option value="1">1 day before</option>
          <option value="3">3 days before</option>
          <option value="7">1 week before</option>
        </SelectField>
      </div>
      <div className="flex justify-end gap-2 pt-2">
        <Button variant="secondary" onClick={onCancel}>Cancel</Button>
        <Button type="submit" loading={submitting}>{rule ? 'Save changes' : 'Schedule'}</Button>
      </div>
    </form>
  );
}
