import { useState, type FormEvent } from 'react';
import { Button } from '../ui/Button';
import { TextField } from '../ui/Field';
import { FormError } from '../ui/States';
import { useFormSubmit } from '../../hooks/useFormSubmit';
import { goalService } from '../../services/goalService';
import type { SavingsGoalResponse } from '../../types/api';
import type { FieldErrors } from '../../utils/errors';
import { todayIso } from '../../utils/dates';
import { isValidAmount } from '../../utils/money';

export function GoalForm({ goal, currency, onSaved, onCancel }: { goal?: SavingsGoalResponse; currency: string; onSaved: () => void; onCancel: () => void }) {
  const goalCurrency = goal?.currency ?? currency;
  const [name, setName] = useState(goal?.name ?? '');
  const [target, setTarget] = useState(goal ? String(goal.targetAmount) : '');
  const [targetDate, setTargetDate] = useState(goal?.targetDate ?? '');
  const { submitting, formError, fieldErrors, submit } = useFormSubmit();

  function validate(): FieldErrors {
    const errors: FieldErrors = {};
    if (!name.trim()) errors.name = 'Name your goal.';
    if (!isValidAmount(target, goalCurrency) || Number(target) <= 0) errors.targetAmount = 'Enter a positive target.';
    if (!goal && targetDate && targetDate < todayIso()) errors.targetDate = 'Choose today or a later date.';
    return errors;
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    await submit(validate(), async () => {
      if (goal) await goalService.update(goal.id, { name: name.trim(), targetAmount: Number(target), targetDate: targetDate || null });
      else await goalService.create({ name: name.trim(), targetAmount: Number(target), currency, targetDate: targetDate || null });
      onSaved();
    });
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4">
      <FormError message={formError} />
      <TextField label="Goal" value={name} onChange={(e) => setName(e.target.value)} error={fieldErrors.name} maxLength={100} placeholder="Emergency fund" />
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <TextField label={`Target (${goalCurrency})`} inputMode="decimal" value={target} onChange={(e) => setTarget(e.target.value)} error={fieldErrors.targetAmount} placeholder="5000.00" />
        <TextField label="Target date (optional)" type="date" value={targetDate} onChange={(e) => setTargetDate(e.target.value)} error={fieldErrors.targetDate} />
      </div>
      <div className="flex justify-end gap-2 pt-2">
        <Button variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" loading={submitting}>
          {goal ? 'Save goal' : 'Create goal'}
        </Button>
      </div>
    </form>
  );
}

export function ContributionForm({ goal, onSaved, onCancel }: { goal: SavingsGoalResponse; onSaved: () => void; onCancel: () => void }) {
  const [amount, setAmount] = useState('');
  const [date, setDate] = useState(todayIso());
  const [note, setNote] = useState('');
  // One key per form instance: double-submits and retries of this contribution are recorded once.
  const [idempotencyKey] = useState(() => crypto.randomUUID());
  const { submitting, formError, fieldErrors, submit } = useFormSubmit();

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const errors: FieldErrors = {};
    if (!isValidAmount(amount, goal.currency) || Number(amount) <= 0) errors.amount = 'Enter a positive amount.';
    if (date > todayIso()) errors.contributionDate = 'Contributions cannot be in the future.';
    await submit(errors, async () => {
      await goalService.contribute(goal.id, { amount: Number(amount), contributionDate: date, note: note.trim() || undefined, idempotencyKey });
      onSaved();
    });
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4">
      <p className="text-sm text-ink-600">
        Record money you have set aside for <strong>{goal.name}</strong>. This tracks your progress; it does not move money between accounts.
      </p>
      <FormError message={formError} />
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <TextField label={`Amount (${goal.currency})`} inputMode="decimal" value={amount} onChange={(e) => setAmount(e.target.value)} error={fieldErrors.amount} />
        <TextField label="Date" type="date" value={date} max={todayIso()} onChange={(e) => setDate(e.target.value)} error={fieldErrors.contributionDate} />
      </div>
      <TextField label="Note (optional)" value={note} onChange={(e) => setNote(e.target.value)} maxLength={255} />
      <div className="flex justify-end gap-2 pt-2">
        <Button variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" loading={submitting}>
          Add contribution
        </Button>
      </div>
    </form>
  );
}
