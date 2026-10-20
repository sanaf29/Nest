import { useState, type FormEvent } from 'react';
import { Button } from '../ui/Button';
import { SelectField, TextField } from '../ui/Field';
import { Icon } from '../ui/Icon';
import { MoneyText } from '../ui/MoneyText';
import { FormError } from '../ui/States';
import { useFormSubmit } from '../../hooks/useFormSubmit';
import { budgetService } from '../../services/budgetService';
import type { BudgetResponse, CategoryResponse, SavingsStrategy } from '../../types/api';
import type { FieldErrors } from '../../utils/errors';
import { fromMinor, isValidAmount, percentOf, sumMoney, toMinor } from '../../utils/money';
import { cn } from '../../utils/cn';

export interface BudgetPreview {
  savingsTarget: number;
  spendable: number;
  allocated: number;
  unallocated: number;
  overAllocated: boolean;
  savingsExceedIncome: boolean;
}

/**
 * Live preview of the plan, using the same rules as the server (half-even rounding, exact cents).
 * The server recomputes everything on save.
 */
export function budgetPreview(income: number, strategy: SavingsStrategy, value: number, limits: number[], currency: string): BudgetPreview {
  const savingsTarget = strategy === 'PERCENTAGE' ? percentOf(income, value, currency) : value;
  const spendable = fromMinor(toMinor(income, currency) - toMinor(savingsTarget, currency), currency);
  const allocated = sumMoney(limits, currency);
  const unallocated = fromMinor(toMinor(spendable, currency) - toMinor(allocated, currency), currency);
  return { savingsTarget, spendable, allocated, unallocated, overAllocated: unallocated < 0, savingsExceedIncome: spendable < 0 };
}

interface Row {
  key: number;
  categoryId: string;
  limit: string;
}

interface BudgetFormProps {
  year: number;
  month: number;
  currency: string;
  budget?: BudgetResponse;
  categories: CategoryResponse[];
  onSaved: (budget: BudgetResponse) => void;
  onCancel: () => void;
}

let nextKey = 1;

export function BudgetForm({ year, month, currency, budget, categories, onSaved, onCancel }: BudgetFormProps) {
  const [income, setIncome] = useState(budget ? String(budget.plannedIncome) : '');
  const [strategy, setStrategy] = useState<SavingsStrategy>(budget?.savingsStrategy ?? 'PERCENTAGE');
  const [savingsValue, setSavingsValue] = useState(budget ? String(budget.savingsValue) : '20');
  const [rows, setRows] = useState<Row[]>(
    budget?.allocations.map((a) => ({ key: nextKey++, categoryId: String(a.categoryId), limit: String(a.limit) })) ?? [],
  );
  const { submitting, formError, fieldErrors, submit } = useFormSubmit();

  const expenseCategories = categories.filter((c) => c.categoryType === 'EXPENSE' && (!c.archived || rows.some((r) => r.categoryId === String(c.id))));
  const numeric = (text: string) => (isValidAmount(text, currency) ? Number(text) : 0);
  const preview = budgetPreview(numeric(income), strategy, strategy === 'PERCENTAGE' ? Number(savingsValue) || 0 : numeric(savingsValue), rows.map((r) => numeric(r.limit)), currency);

  const updateRow = (key: number, patch: Partial<Row>) => setRows((rs) => rs.map((r) => (r.key === key ? { ...r, ...patch } : r)));

  function validate(): FieldErrors {
    const errors: FieldErrors = {};
    if (!isValidAmount(income, currency)) errors.plannedIncome = 'Enter your expected income for the month.';
    if (strategy === 'PERCENTAGE') {
      const p = Number(savingsValue);
      if (!/^\d+(\.\d{1,2})?$/.test(savingsValue) || p < 0 || p > 100) errors.savingsValue = 'Enter a percentage from 0 to 100.';
    } else if (!isValidAmount(savingsValue, currency)) {
      errors.savingsValue = 'Enter an amount.';
    }
    if (preview.savingsExceedIncome) errors.savingsValue = 'Savings cannot exceed planned income.';
    const seen = new Set<string>();
    rows.forEach((row, index) => {
      if (!row.categoryId) errors[`allocations[${index}].categoryId`] = 'Choose a category.';
      else if (seen.has(row.categoryId)) errors[`allocations[${index}].categoryId`] = 'Already used above.';
      seen.add(row.categoryId);
      if (!isValidAmount(row.limit, currency) || Number(row.limit) <= 0) errors[`allocations[${index}].limit`] = 'Enter a positive limit.';
    });
    if (preview.overAllocated) errors.allocations = 'Category limits exceed what is left after savings.';
    return errors;
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    await submit(validate(), async () => {
      const plan = {
        plannedIncome: Number(income),
        savingsStrategy: strategy,
        savingsValue: Number(savingsValue),
        allocations: rows.map((r) => ({ categoryId: Number(r.categoryId), limit: Number(r.limit) })),
      };
      const saved = budget ? await budgetService.update(budget.id, plan) : await budgetService.create({ ...plan, year, month, currency });
      onSaved(saved);
    });
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-5">
      <FormError message={formError} />
      <TextField label={`Planned income (${currency})`} inputMode="decimal" value={income} onChange={(e) => setIncome(e.target.value)} error={fieldErrors.plannedIncome} placeholder="3000.00" />

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <SelectField label="Savings" value={strategy} onChange={(e) => setStrategy(e.target.value as SavingsStrategy)}>
          <option value="PERCENTAGE">Percentage of income</option>
          <option value="FIXED_AMOUNT">Fixed amount</option>
        </SelectField>
        <TextField
          label={strategy === 'PERCENTAGE' ? 'Savings %' : `Savings (${currency})`}
          inputMode="decimal"
          value={savingsValue}
          onChange={(e) => setSavingsValue(e.target.value)}
          error={fieldErrors.savingsValue}
        />
      </div>

      <dl aria-label="Plan preview" className="grid grid-cols-2 gap-3 rounded-xl bg-ink-50 p-4 text-sm sm:grid-cols-4">
        {[
          ['Savings target', preview.savingsTarget],
          ['Available to spend', preview.spendable],
          ['Allocated', preview.allocated],
          ['Unallocated', preview.unallocated],
        ].map(([label, value]) => (
          <div key={label as string}>
            <dt className="text-ink-500">{label}</dt>
            <dd className={cn('font-semibold', label === 'Unallocated' && preview.overAllocated && 'text-negative-600')}>
              <MoneyText amount={value as number} currency={currency} />
            </dd>
          </div>
        ))}
      </dl>

      <fieldset className="flex flex-col gap-3">
        <legend className="mb-1 text-sm font-medium text-ink-700">Category limits</legend>
        {rows.length === 0 && <p className="text-sm text-ink-500">Optional: set spending limits for the categories you want to watch.</p>}
        {rows.map((row, index) => (
          <div key={row.key} className="grid grid-cols-[1fr_8rem_auto] items-start gap-2">
            <SelectField label={`Category ${index + 1}`} value={row.categoryId} onChange={(e) => updateRow(row.key, { categoryId: e.target.value })} error={fieldErrors[`allocations[${index}].categoryId`]}>
              <option value="">Choose…</option>
              {expenseCategories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </SelectField>
            <TextField label="Limit" inputMode="decimal" value={row.limit} onChange={(e) => updateRow(row.key, { limit: e.target.value })} error={fieldErrors[`allocations[${index}].limit`]} />
            <button type="button" onClick={() => setRows((rs) => rs.filter((r) => r.key !== row.key))} className="mt-6 rounded-lg p-2 text-ink-500 hover:bg-ink-100" aria-label={`Remove category ${index + 1}`}>
              <Icon name="trash" className="h-4 w-4" />
            </button>
          </div>
        ))}
        {fieldErrors.allocations && (
          <p role="alert" className="text-xs text-negative-600">
            {fieldErrors.allocations}
          </p>
        )}
        <Button variant="secondary" size="sm" icon={<Icon name="plus" className="h-4 w-4" />} onClick={() => setRows((rs) => [...rs, { key: nextKey++, categoryId: '', limit: '' }])} className="self-start">
          Add category limit
        </Button>
      </fieldset>

      <div className="flex justify-end gap-2 pt-2">
        <Button variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" loading={submitting}>
          {budget ? 'Save budget' : 'Create budget'}
        </Button>
      </div>
    </form>
  );
}
