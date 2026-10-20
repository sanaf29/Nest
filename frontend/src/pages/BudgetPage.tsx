import { useState } from 'react';
import { BudgetForm } from '../components/forms/BudgetForm';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { ConfirmDialog } from '../components/ui/ConfirmDialog';
import { Icon } from '../components/ui/Icon';
import { MoneyText } from '../components/ui/MoneyText';
import { MonthPicker } from '../components/ui/MonthPicker';
import { PageHeader } from '../components/ui/PageHeader';
import { ProgressBar } from '../components/ui/ProgressBar';
import { EmptyState, ErrorState, LoadingState } from '../components/ui/States';
import { useAsync } from '../hooks/useAsync';
import { useAuth } from '../hooks/useAuth';
import { budgetService } from '../services/budgetService';
import { categoryService } from '../services/categoryService';
import type { BudgetStatus } from '../types/api';
import { currentYearMonth, monthLabel } from '../utils/dates';
import { formatMoney } from '../utils/money';

const statusBadge: Record<BudgetStatus, { tone: 'positive' | 'warning' | 'negative'; label: string }> = {
  ON_TRACK: { tone: 'positive', label: 'On track' },
  NEAR_LIMIT: { tone: 'warning', label: 'Near limit' },
  EXCEEDED: { tone: 'negative', label: 'Over limit' },
};

export function BudgetPage() {
  const { user } = useAuth();
  const currency = user?.preferredCurrency ?? 'CAD';
  const [month, setMonth] = useState(currentYearMonth);
  const [mode, setMode] = useState<'view' | 'edit'>('view');
  const [deleting, setDeleting] = useState(false);

  const budget = useAsync(() => budgetService.forMonth(month.year, month.month, currency), [month.year, month.month, currency]);
  const summary = useAsync(
    () => (budget.data ? budgetService.summary(budget.data.id) : Promise.resolve(null)),
    [budget.data?.id, budget.data?.updatedAt],
  );
  const categories = useAsync(() => categoryService.list('EXPENSE'), []);

  const changeMonth = (next: typeof month) => {
    setMode('view');
    setMonth(next);
  };

  return (
    <>
      <PageHeader title="Budget planner" description={`Your plan for ${monthLabel(month)} in ${currency}.`} actions={<MonthPicker value={month} onChange={changeMonth} />} />

      {budget.loading && budget.data === undefined ? (
        <LoadingState />
      ) : budget.error ? (
        <ErrorState error={budget.error} onRetry={budget.reload} />
      ) : mode === 'edit' || budget.data === null ? (
        budget.data === null && mode === 'view' ? (
          <Card>
            <EmptyState icon="pie" title={`No budget for ${monthLabel(month)}`} action={<Button onClick={() => setMode('edit')}>Create budget</Button>}>
              Decide how much to save first, then give your spending categories a limit.
            </EmptyState>
          </Card>
        ) : (
          <Card title={budget.data ? 'Edit budget' : 'Create budget'}>
            <BudgetForm
              year={month.year}
              month={month.month}
              currency={currency}
              budget={budget.data ?? undefined}
              categories={categories.data ?? []}
              onSaved={() => {
                setMode('view');
                budget.reload();
              }}
              onCancel={() => setMode('view')}
            />
          </Card>
        )
      ) : budget.data && summary.data ? (
        <div className="flex flex-col gap-6">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {[
              ['Planned income', summary.data.plannedIncome, `Actual ${formatMoney(summary.data.actualIncome, currency)}`],
              ['Savings target', summary.data.savingsTarget, budget.data.savingsStrategy === 'PERCENTAGE' ? `${budget.data.savingsValue}% of income` : 'Fixed amount'],
              ['Spent', summary.data.actualExpenses, summary.data.percentUsed === null ? undefined : `${summary.data.percentUsed.toFixed(1)}% of ${formatMoney(summary.data.spendableAmount, currency)}`],
              ['Remaining', summary.data.remaining, summary.data.remaining < 0 ? 'Over budget' : 'Left to spend this month'],
            ].map(([label, value, hint]) => (
              <div key={label as string} className="rounded-2xl bg-white p-5 shadow-sm ring-1 ring-ink-200/70">
                <p className="text-sm text-ink-500">{label}</p>
                <p className="mt-1 text-2xl font-semibold">
                  <MoneyText amount={value as number} currency={currency} signed={label === 'Remaining' && (value as number) < 0} />
                </p>
                {hint && <p className="mt-1 text-xs text-ink-500">{hint}</p>}
              </div>
            ))}
          </div>

          <Card title="Savings progress" description="Income minus expenses so far, compared with your savings target.">
            {summary.data.savingsProgressPercent === null ? (
              <p className="text-sm text-ink-500">No savings target this month.</p>
            ) : (
              <div className="flex flex-col gap-2">
                <ProgressBar value={summary.data.savingsProgressPercent} label="Savings progress" tone={summary.data.savingsProgressPercent >= 100 ? 'positive' : 'brand'} />
                <p className="text-sm text-ink-600">
                  <MoneyText amount={summary.data.netCashFlow} currency={currency} signed /> saved of <MoneyText amount={summary.data.savingsTarget} currency={currency} /> ({summary.data.savingsProgressPercent.toFixed(0)}%)
                </p>
              </div>
            )}
          </Card>

          <Card
            title="Category limits"
            bodyClassName="p-0"
            action={
              <div className="flex gap-2">
                <Button size="sm" variant="secondary" icon={<Icon name="edit" className="h-4 w-4" />} onClick={() => setMode('edit')}>
                  Edit
                </Button>
                <Button size="sm" variant="ghost" onClick={() => setDeleting(true)} aria-label="Delete budget">
                  <Icon name="trash" className="h-4 w-4" />
                </Button>
              </div>
            }
          >
            {summary.data.categories.length === 0 ? (
              <EmptyState icon="pie" title="No category limits" action={<Button size="sm" onClick={() => setMode('edit')}>Add limits</Button>}>
                Limits show how each category is tracking during the month.
              </EmptyState>
            ) : (
              <ul className="divide-y divide-ink-100">
                {summary.data.categories.map((line) => (
                  <li key={line.categoryId} className="px-5 py-4">
                    <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
                      <span className="flex items-center gap-2 text-sm font-medium text-ink-800">
                        <span className="h-2.5 w-2.5 rounded-full" style={{ backgroundColor: line.color }} aria-hidden="true" />
                        {line.categoryName}
                      </span>
                      <Badge tone={statusBadge[line.status].tone}>{statusBadge[line.status].label}</Badge>
                    </div>
                    <ProgressBar value={line.percentUsed} label={`${line.categoryName} spending`} tone={line.status === 'EXCEEDED' ? 'negative' : line.status === 'NEAR_LIMIT' ? 'warning' : 'brand'} />
                    <p className="mt-1.5 flex justify-between text-xs text-ink-500">
                      <span>
                        <MoneyText amount={line.spent} currency={currency} /> of <MoneyText amount={line.limit} currency={currency} />
                      </span>
                      <span>
                        <MoneyText amount={line.remaining} currency={currency} signed={line.remaining < 0} /> {line.remaining < 0 ? 'over' : 'left'}
                      </span>
                    </p>
                  </li>
                ))}
                {summary.data.unbudgetedSpending > 0 && (
                  <li className="px-5 py-3 text-sm text-ink-600">
                    <MoneyText amount={summary.data.unbudgetedSpending} currency={currency} /> spent in categories without a limit.
                  </li>
                )}
              </ul>
            )}
          </Card>
        </div>
      ) : (
        <LoadingState />
      )}

      <ConfirmDialog
        open={deleting}
        title="Delete this budget?"
        message="Your plan for this month will be removed. Transactions are not affected."
        confirmLabel="Delete budget"
        danger
        onConfirm={async () => {
          await budgetService.remove(budget.data!.id);
          budget.reload();
        }}
        onClose={() => setDeleting(false)}
      />
    </>
  );
}
