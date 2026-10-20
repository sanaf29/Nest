import { useState, type FormEvent } from 'react';
import { Button } from '../ui/Button';
import { SelectField, TextAreaField, TextField } from '../ui/Field';
import { FormError } from '../ui/States';
import { useFormSubmit } from '../../hooks/useFormSubmit';
import { reminderService } from '../../services/reminderService';
import type { CategoryResponse, ReminderResponse, ReminderType, SavingsGoalResponse } from '../../types/api';
import type { FieldErrors } from '../../utils/errors';

export const reminderTypeLabels: Record<ReminderType, string> = {
  BILL_PAYMENT: 'Bill payment',
  SUBSCRIPTION_RENEWAL: 'Subscription renewal',
  PARKING: 'Parking',
  SAVINGS_GOAL_DEADLINE: 'Savings goal deadline',
  BUDGET_THRESHOLD: 'Budget alert',
  CUSTOM: 'Custom',
};

const offsets = [
  [0, 'At the time'],
  [10, '10 minutes before'],
  [15, '15 minutes before'],
  [30, '30 minutes before'],
  [60, '1 hour before'],
  [1440, '1 day before'],
  [4320, '3 days before'],
  [10080, '1 week before'],
] as const;

/** ISO instant → value for <input type="datetime-local"> in the user's own time zone. */
export function toLocalInput(iso: string | null | undefined): string {
  if (!iso) return '';
  const d = new Date(iso);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

interface Props {
  reminder?: ReminderResponse;
  goals: SavingsGoalResponse[];
  categories: CategoryResponse[];
  onSaved: () => void;
  onCancel: () => void;
}

export function ReminderForm({ reminder, goals, categories, onSaved, onCancel }: Props) {
  const [type, setType] = useState<ReminderType>(reminder?.type ?? 'BILL_PAYMENT');
  const [title, setTitle] = useState(reminder?.title ?? '');
  const [notes, setNotes] = useState(reminder?.notes ?? '');
  const [when, setWhen] = useState(toLocalInput(reminder?.dueAt));
  const [offset, setOffset] = useState(String(reminder?.offsetMinutes ?? 0));
  const [location, setLocation] = useState(reminder?.location ?? '');
  const [goalId, setGoalId] = useState(String(reminder?.savingsGoalId ?? ''));
  const [categoryId, setCategoryId] = useState(String(reminder?.categoryId ?? ''));
  const [threshold, setThreshold] = useState(String(reminder?.thresholdPercent ?? 80));
  const { submitting, formError, fieldErrors, submit } = useFormSubmit();

  const timed = type !== 'BUDGET_THRESHOLD';

  function chooseType(next: ReminderType) {
    setType(next);
    if (next === 'PARKING') setOffset('15');
    if (!title || Object.values(reminderTypeLabels).includes(title)) setTitle(next === 'CUSTOM' ? '' : reminderTypeLabels[next]);
  }

  function validate(): FieldErrors {
    const e: FieldErrors = {};
    if (!title.trim()) e.title = 'Give it a title.';
    if (timed) {
      if (!when) e.dueAt = type === 'PARKING' ? 'When does your parking expire?' : 'Choose a date and time.';
      else if (new Date(when) <= new Date()) e.dueAt = 'Choose a time in the future.';
    } else {
      const t = Number(threshold);
      if (!Number.isInteger(t) || t < 1 || t > 200) e.thresholdPercent = 'Enter a whole percentage from 1 to 200.';
    }
    if (type === 'PARKING' && !location.trim()) e.location = 'Where did you park?';
    if (type === 'SAVINGS_GOAL_DEADLINE' && !goalId) e.savingsGoalId = 'Choose a goal.';
    return e;
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    await submit(validate(), async () => {
      const request = {
        type,
        title: title.trim(),
        notes: notes.trim() || undefined,
        dueAt: timed ? new Date(when).toISOString() : null,
        offsetMinutes: timed ? Number(offset) : 0,
        location: type === 'PARKING' ? location.trim() : undefined,
        savingsGoalId: type === 'SAVINGS_GOAL_DEADLINE' ? Number(goalId) : null,
        categoryId: type === 'BUDGET_THRESHOLD' && categoryId ? Number(categoryId) : null,
        thresholdPercent: timed ? null : Number(threshold),
      };
      if (reminder) await reminderService.update(reminder.id, request);
      else await reminderService.create(request);
      onSaved();
    });
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4">
      <FormError message={formError} />
      <SelectField label="Kind" value={type} onChange={(e) => chooseType(e.target.value as ReminderType)} disabled={!!reminder}>
        {Object.entries(reminderTypeLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
      </SelectField>
      <TextField label="Title" value={title} onChange={(e) => setTitle(e.target.value)} error={fieldErrors.title} maxLength={150} />
      {type === 'PARKING' && (
        <TextField label="Where you parked" value={location} onChange={(e) => setLocation(e.target.value)} error={fieldErrors.location}
          placeholder="Level 2, spot B14 — Queen St garage" maxLength={200} />
      )}
      {type === 'SAVINGS_GOAL_DEADLINE' && (
        <SelectField label="Goal" value={goalId} onChange={(e) => setGoalId(e.target.value)} error={fieldErrors.savingsGoalId}>
          <option value="">Choose…</option>
          {goals.map((g) => <option key={g.id} value={g.id}>{g.name}</option>)}
        </SelectField>
      )}
      {timed ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <TextField label={type === 'PARKING' ? 'Parking expires' : 'Due'} type="datetime-local" value={when} onChange={(e) => setWhen(e.target.value)} error={fieldErrors.dueAt} />
          <SelectField label="Notify me" value={offset} onChange={(e) => setOffset(e.target.value)}>
            {offsets.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </SelectField>
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <SelectField label="Watch" value={categoryId} onChange={(e) => setCategoryId(e.target.value)} hint="Uses this month's budget in your preferred currency.">
            <option value="">Whole budget</option>
            {categories.filter((c) => c.categoryType === 'EXPENSE' && !c.archived).map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </SelectField>
          <TextField label="Alert at (% of budget)" inputMode="numeric" value={threshold} onChange={(e) => setThreshold(e.target.value)} error={fieldErrors.thresholdPercent} />
        </div>
      )}
      <TextAreaField label="Notes (optional)" rows={2} value={notes} onChange={(e) => setNotes(e.target.value)} maxLength={500} />
      <div className="flex justify-end gap-2 pt-2">
        <Button variant="secondary" onClick={onCancel}>Cancel</Button>
        <Button type="submit" loading={submitting}>{reminder ? 'Save' : 'Create reminder'}</Button>
      </div>
    </form>
  );
}
