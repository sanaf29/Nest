import { useMemo, useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router';
import { frequencyLabels } from '../components/forms/RecurringForm';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { CategoryIcon } from '../components/ui/CategoryIcon';
import { SelectField, TextField } from '../components/ui/Field';
import { Icon } from '../components/ui/Icon';
import { SegmentedControl } from '../components/ui/SegmentedControl';
import { EmptyState, ErrorState, FormError, LoadingState } from '../components/ui/States';
import { Switch } from '../components/ui/Switch';
import { useFormSubmit } from '../hooks/useFormSubmit';
import { useReferenceData } from '../hooks/useReferenceData';
import { recurringService } from '../services/recurringService';
import { transactionService } from '../services/transactionService';
import type { Frequency } from '../types/api';
import { tint } from '../utils/categoryStyle';
import { cn } from '../utils/cn';
import { todayIso } from '../utils/dates';
import type { FieldErrors } from '../utils/errors';
import { isValidAmount } from '../utils/money';

type EntryType = 'EXPENSE' | 'INCOME';

/** The currency symbol on its own, e.g. "$" for CAD in en-CA or "CA$" in en-US. */
function currencySymbol(currency: string): string {
  return new Intl.NumberFormat(undefined, { style: 'currency', currency }).formatToParts(0).find((p) => p.type === 'currency')?.value ?? currency;
}

/**
 * The full-screen "add" flow behind the + button: amount first, then type, category, account,
 * date, and notes. Turning on "Repeat" schedules a recurring rule instead; its first occurrence
 * is recorded right away when the date is today or earlier.
 */
export function AddTransactionPage() {
  const reference = useReferenceData();
  const navigate = useNavigate();
  const location = useLocation();
  const [params] = useSearchParams();
  const [type, setType] = useState<EntryType>(params.get('type') === 'income' ? 'INCOME' : 'EXPENSE');
  const [amount, setAmount] = useState('');
  const [categoryId, setCategoryId] = useState<number | null>(null);
  const [accountId, setAccountId] = useState('');
  const [date, setDate] = useState(todayIso());
  const [notes, setNotes] = useState('');
  const [repeat, setRepeat] = useState(false);
  const [frequency, setFrequency] = useState<Frequency>('MONTHLY');
  const { submitting, formError, fieldErrors, submit } = useFormSubmit();

  const accountChoice = accountId || String(reference.accounts[0]?.id ?? '');
  const account = reference.accounts.find((a) => String(a.id) === accountChoice);
  const currency = account?.currency ?? 'CAD';
  const options = useMemo(() => reference.categories.filter((c) => c.categoryType === type && !c.archived), [reference.categories, type]);

  function leave() {
    // Go back to where + was pressed; if /add was opened directly, go home.
    if (location.key !== 'default') navigate(-1);
    else navigate('/', { replace: true });
  }

  function chooseType(next: EntryType) {
    setType(next);
    setCategoryId(null);
  }

  function validate(): FieldErrors {
    const errors: FieldErrors = {};
    if (!isValidAmount(amount, currency) || Number(amount) <= 0) errors.amount = `Enter a positive amount${currency === 'JPY' ? '' : ', e.g. 84.37'}.`;
    if (categoryId === null) errors.categoryId = 'Choose a category.';
    if (!account) errors.accountId = 'Choose an account.';
    if (!date) errors.transactionDate = 'Choose a date.';
    if (repeat && !notes.trim()) errors.description = 'Name the repeating transaction, e.g. Rent or Netflix.';
    return errors;
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    await submit(validate(), async () => {
      const common = { accountId: account!.id, categoryId: categoryId!, type, amount: Number(amount) };
      if (repeat) {
        await recurringService.create({ ...common, description: notes.trim(), frequency, startDate: date });
      } else {
        await transactionService.create({ ...common, transactionDate: date, description: notes.trim() || undefined });
      }
      leave();
    });
  }

  if (reference.loading && reference.accounts.length === 0) return <LoadingState />;
  if (reference.error) return <ErrorState error={reference.error} onRetry={reference.reload} />;

  return (
    <div className="mx-auto max-w-xl">
      <div className="mb-5 flex items-center gap-3">
        <button type="button" onClick={leave} className="rounded-full bg-surface p-2 text-ink-600 ring-1 ring-line/50 hover:text-ink-900" aria-label="Back">
          <Icon name="chevronLeft" />
        </button>
        <h1 className="text-xl font-bold tracking-tight text-ink-900">Add transaction</h1>
      </div>

      {reference.accounts.length === 0 ? (
        <Card>
          <EmptyState icon="wallet" title="Add an account first" action={<Link to="/accounts" className="text-sm font-bold text-brand-700 hover:underline">Go to accounts</Link>}>
            Every transaction belongs to an account: chequing, savings, cash, or a credit card.
          </EmptyState>
        </Card>
      ) : (
        <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-5">
          <FormError message={formError} />

          <div className="card-hero flex animate-rise flex-col items-center gap-3 rounded-3xl px-5 pt-6 pb-5 text-center shadow-card">
            <span className="flex h-14 w-14 items-center justify-center rounded-2xl bg-white/15 ring-1 ring-white/20" aria-hidden="true">
              <Icon name="cash" className="h-7 w-7" />
            </span>
            <label htmlFor="amount" className="text-xs font-semibold tracking-wide text-white/75 uppercase">
              Amount ({currency})
            </label>
            <div className="flex items-baseline justify-center gap-1">
              <span className="text-2xl font-bold text-white/70" aria-hidden="true">{currencySymbol(currency)}</span>
              <input
                id="amount"
                inputMode="decimal"
                autoComplete="off"
                placeholder="0.00"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                aria-invalid={fieldErrors.amount ? true : undefined}
                aria-describedby={fieldErrors.amount ? 'amount-error' : undefined}
                style={{ width: `${Math.max(4, amount.length + 0.5)}ch` }}
                className="max-w-56 min-w-0 bg-transparent text-center text-5xl font-extrabold tracking-tight text-white tabular-nums placeholder:text-white/35 focus:outline-none"
              />
            </div>
            {fieldErrors.amount && (
              <p id="amount-error" role="alert" className="rounded-full bg-black/25 px-3 py-1 text-xs font-medium text-white">
                {fieldErrors.amount}
              </p>
            )}
            <SegmentedControl
              label="Type"
              value={type}
              onChange={chooseType}
              className="mt-1 w-full max-w-64 bg-black/20 ring-white/10"
              options={[
                { value: 'EXPENSE', label: 'Expense', activeClassName: 'bg-brand-600 text-on-brand' },
                { value: 'INCOME', label: 'Income', activeClassName: 'bg-positive-600 text-on-brand' },
              ]}
            />
          </div>

          <fieldset>
            <legend className="mb-2 text-xs font-semibold tracking-wide text-ink-500">Category</legend>
            <div role="radiogroup" aria-label="Category" aria-describedby={fieldErrors.categoryId ? 'category-error' : undefined} className="grid grid-cols-3 gap-2 sm:grid-cols-4">
              {options.map((c) => {
                const selected = categoryId === c.id;
                return (
                  <button
                    key={c.id}
                    type="button"
                    role="radio"
                    aria-checked={selected}
                    onClick={() => setCategoryId(c.id)}
                    className={cn(
                      'flex flex-col items-center gap-1.5 rounded-2xl px-2 py-3 text-center text-xs font-semibold ring-1 transition-[transform,background-color] active:scale-95',
                      selected ? 'text-ink-900 ring-2 ring-brand-500' : 'bg-surface text-ink-600 ring-line/40 hover:bg-surface-2',
                    )}
                    style={selected ? { backgroundColor: tint(c.color, 0.22) } : undefined}
                  >
                    <CategoryIcon name={c.name} color={c.color} className="h-9 w-9 rounded-xl" />
                    <span className="line-clamp-2 leading-tight">{c.name}</span>
                  </button>
                );
              })}
            </div>
            {fieldErrors.categoryId && (
              <p id="category-error" role="alert" className="mt-1.5 text-xs text-negative-600">
                {fieldErrors.categoryId}
              </p>
            )}
          </fieldset>

          <Card bodyClassName="flex flex-col gap-4">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <SelectField label="Account" value={accountChoice} onChange={(e) => setAccountId(e.target.value)} error={fieldErrors.accountId}>
                {reference.accounts.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.name} ({a.currency})
                  </option>
                ))}
              </SelectField>
              <TextField label={repeat ? 'First date' : 'Date'} type="date" value={date} onChange={(e) => setDate(e.target.value)} error={fieldErrors.transactionDate} />
            </div>
            <TextField
              label={repeat ? 'Name' : 'Notes (optional)'}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              error={fieldErrors.description}
              maxLength={255}
              placeholder={type === 'EXPENSE' ? 'Weekly groceries' : 'October pay'}
            />
            <Switch checked={repeat} onChange={setRepeat} label="Repeat this transaction" description="Nest records it for you on schedule." />
            {repeat && (
              <SelectField
                label="Repeats"
                value={frequency}
                onChange={(e) => setFrequency(e.target.value as Frequency)}
                hint={date <= todayIso() ? 'Occurrences up to today are recorded right away.' : 'The first one is recorded on that date.'}
              >
                {Object.entries(frequencyLabels).map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </SelectField>
            )}
          </Card>

          <Button type="submit" size="lg" loading={submitting} className="w-full">
            Save transaction
          </Button>
        </form>
      )}
    </div>
  );
}
