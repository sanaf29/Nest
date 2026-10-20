import { useEffect, useRef, useState } from 'react';
import { BudgetForm } from '../components/forms/BudgetForm';
import { TransactionList } from '../components/TransactionList';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { CategoryIcon } from '../components/ui/CategoryIcon';
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
import { dashboardService } from '../services/dashboardService';
import { transactionService } from '../services/transactionService';
import type { BudgetStatus } from '../types/api';
import { pastel, tint } from '../utils/categoryStyle';
import { cn } from '../utils/cn';
import { currentYearMonth, monthLabel, monthRange, type YearMonth } from '../utils/dates';
import { formatMoney } from '../utils/money';

const statusBadge: Record<BudgetStatus, { tone: 'positive' | 'warning' | 'negative'; label: string }> = {
  ON_TRACK: { tone: 'positive', label: 'On track' },
  NEAR_LIMIT: { tone: 'warning', label: 'Near limit' },
  EXCEEDED: { tone: 'negative', label: 'Over limit' },
};

/** One category this month: its limit (if budgeted) and what was spent. */
interface CategoryLine {
  categoryId: number;
  name: string;
  color: string;
  spent: number;
  limit: number | null;
  remaining: number | null;
  percentUsed: number | null;
  status: BudgetStatus | null;
}

function CategoryCard({ line, currency, selected, onSelect, index }: { line: CategoryLine; currency: string; selected: boolean; onSelect: () => void; index: number }) {
  return (
    <li
      className={cn(
        'relative flex animate-rise flex-col gap-3 rounded-3xl p-4 ring-1 transition-[transform,box-shadow] duration-200 hover:-translate-y-0.5 hover:shadow-lift',
        selected ? '-translate-y-0.5 shadow-lift ring-2 ring-brand-500' : 'shadow-card ring-line/40',
      )}
      style={{ animationDelay: `${Math.min(index, 8) * 30}ms`, background: `linear-gradient(160deg, ${tint(line.color, selected ? 0.3 : 0.16)}, var(--color-surface) 70%)` }}
    >
      <div className="flex items-start justify-between gap-2">
        <CategoryIcon name={line.name} color={line.color} />
        {line.status ? <Badge tone={statusBadge[line.status].tone}>{statusBadge[line.status].label}</Badge> : <Badge>No limit</Badge>}
      </div>
      <div className="min-w-0">
        {/* The button's ::after covers the whole card, so the card is one large tap target. */}
        <button type="button" onClick={onSelect} aria-pressed={selected} className="block w-full truncate text-left text-sm font-semibold text-ink-700 after:absolute after:inset-0 after:rounded-3xl focus-visible:outline-none focus-visible:after:outline-2 focus-visible:after:outline-brand-500">
          {line.name}
        </button>
        <p className="text-lg font-bold tracking-tight text-ink-900 tabular-nums">{formatMoney(line.spent, currency)}</p>
        <p className="text-xs text-ink-500">{line.limit !== null ? `of ${formatMoney(line.limit, currency)}` : 'spent this month'}</p>
      </div>
      {line.percentUsed !== null && (
        <ProgressBar
          value={line.percentUsed}
          label={`${line.name} spending`}
          color={line.status === 'ON_TRACK' ? pastel(line.color) : undefined}
          tone={line.status === 'EXCEEDED' ? 'negative' : 'warning'}
        />
      )}
    </li>
  );
}

function CategoryDetail({ line, currency, month, onEdit }: { line: CategoryLine; currency: string; month: YearMonth; onEdit: () => void }) {
  const recent = useAsync(
    () => transactionService.search({ ...monthRange(month), categoryId: line.categoryId, sort: 'transactionDate,desc', page: 0, size: 5 }),
    [line.categoryId, month.year, month.month],
  );
  const stats: [string, number | null][] = [
    ['Spent', line.spent],
    ['Monthly budget', line.limit],
    ['Remaining', line.remaining],
  ];
  return (
    <Card className="lg:sticky lg:top-6" bodyClassName="flex flex-col gap-5">
      <div className="flex items-center justify-between gap-3">
        <div className="flex min-w-0 items-center gap-3">
          <CategoryIcon name={line.name} color={line.color} className="h-12 w-12" />
          <div className="min-w-0">
            <h2 className="truncate text-lg font-bold text-ink-900">{line.name}</h2>
            <p className="text-xs text-ink-500">{monthLabel(month)}</p>
          </div>
        </div>
        <Button size="sm" variant="secondary" icon={<Icon name="edit" className="h-4 w-4" />} onClick={onEdit}>
          Edit budget
        </Button>
      </div>
      <dl className="grid grid-cols-1 gap-2 sm:grid-cols-3">
        {stats.map(([label, value]) => (
          <div key={label} className="flex items-center justify-between gap-2 rounded-2xl bg-surface-2 px-3 py-2.5 ring-1 ring-line/40 sm:block sm:py-3">
            <dt className="text-[11px] font-semibold tracking-wide text-ink-500 uppercase">{label}</dt>
            <dd className={cn('truncate text-sm font-bold sm:mt-1 tabular-nums', value !== null && value < 0 ? 'text-negative-600' : 'text-ink-900')}>
              {value === null ? '—' : formatMoney(value, currency)}
            </dd>
          </div>
        ))}
      </dl>
      {line.percentUsed !== null ? (
        <div>
          <ProgressBar
            value={line.percentUsed}
            label={`${line.name} budget used`}
            color={line.status === 'ON_TRACK' ? pastel(line.color) : undefined}
            tone={line.status === 'EXCEEDED' ? 'negative' : 'warning'}
            className="h-2"
          />
          <p className="mt-1.5 text-xs text-ink-500">{line.percentUsed.toFixed(0)}% of this month’s limit used</p>
        </div>
      ) : (
        <p className="rounded-2xl bg-brand-50 px-4 py-3 text-sm text-ink-700">No limit set. Edit the budget to give {line.name} a monthly limit.</p>
      )}
      <div>
        <h3 className="mb-1 text-sm font-semibold text-ink-900">Recent in {line.name}</h3>
        {recent.error ? (
          <ErrorState error={recent.error} onRetry={recent.reload} />
        ) : recent.data && recent.data.content.length > 0 ? (
          <div className="-mx-3">
            <TransactionList transactions={recent.data.content} showAccount={false} />
          </div>
        ) : recent.loading ? (
          <LoadingState />
        ) : (
          <p className="text-sm text-ink-500">Nothing spent here this month.</p>
        )}
      </div>
    </Card>
  );
}

export function BudgetPage() {
  const { user } = useAuth();
  const currency = user?.preferredCurrency ?? 'CAD';
  const [month, setMonth] = useState(currentYearMonth);
  const [mode, setMode] = useState<'view' | 'edit'>('view');
  const [deleting, setDeleting] = useState(false);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const detailRef = useRef<HTMLDivElement>(null);

  const budget = useAsync(() => budgetService.forMonth(month.year, month.month, currency), [month.year, month.month, currency]);
  const summary = useAsync(
    () => (budget.data ? budgetService.summary(budget.data.id) : Promise.resolve(null)),
    [budget.data?.id, budget.data?.updatedAt],
  );
  const spending = useAsync(() => dashboardService.spendingByCategory(month.year, month.month, currency), [month.year, month.month, currency]);
  const categories = useAsync(() => categoryService.list('EXPENSE'), []);

  const changeMonth = (next: YearMonth) => {
    setMode('view');
    setSelectedId(null);
    setMonth(next);
  };

  // Budgeted categories first (by how much of the limit is used), then other spending.
  const lines: CategoryLine[] = [
    ...(summary.data?.categories ?? [])
      .map((c) => ({ categoryId: c.categoryId, name: c.categoryName, color: c.color, spent: c.spent, limit: c.limit, remaining: c.remaining, percentUsed: c.percentUsed, status: c.status }))
      .sort((a, b) => b.percentUsed - a.percentUsed),
    ...(spending.data?.categories ?? [])
      .filter((c) => !summary.data?.categories.some((b) => b.categoryId === c.categoryId))
      .map((c) => ({ categoryId: c.categoryId, name: c.categoryName, color: c.color, spent: c.amount, limit: null, remaining: null, percentUsed: null, status: null })),
  ];
  const selected = lines.find((l) => l.categoryId === selectedId) ?? null;

  useEffect(() => {
    if (selectedId !== null && window.matchMedia?.('(max-width: 1023px)').matches) {
      detailRef.current?.scrollIntoView?.({ behavior: 'smooth', block: 'start' });
    }
  }, [selectedId]);

  const grid = lines.length > 0 && (
    <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3" aria-label="Categories">
      {lines.map((line, i) => (
        <CategoryCard
          key={line.categoryId}
          index={i}
          line={line}
          currency={currency}
          selected={selectedId === line.categoryId}
          onSelect={() => setSelectedId((id) => (id === line.categoryId ? null : line.categoryId))}
        />
      ))}
    </ul>
  );

  return (
    <>
      <PageHeader title="Budget" description={`Categories and limits for ${monthLabel(month)} in ${currency}.`} actions={<MonthPicker value={month} onChange={changeMonth} />} />

      {budget.loading && budget.data === undefined ? (
        <LoadingState />
      ) : budget.error ? (
        <ErrorState error={budget.error} onRetry={budget.reload} />
      ) : mode === 'edit' ? (
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
      ) : budget.data === null ? (
        <div className="flex flex-col gap-6">
          <Card>
            <EmptyState icon="pie" title={`No budget for ${monthLabel(month)}`} action={<Button onClick={() => setMode('edit')}>Create budget</Button>}>
              Decide how much to save first, then give your spending categories a limit.
            </EmptyState>
          </Card>
          {grid && (
            <section className="grid grid-cols-1 gap-6 lg:grid-cols-5">
              <div className="lg:col-span-3">
                <h2 className="mb-3 px-1 text-base font-semibold text-ink-900">Spending so far</h2>
                {grid}
              </div>
              <div ref={detailRef} className="scroll-mt-20 lg:col-span-2">
                {selected && <CategoryDetail line={selected} currency={currency} month={month} onEdit={() => setMode('edit')} />}
              </div>
            </section>
          )}
        </div>
      ) : budget.data && summary.data ? (
        <div className="flex flex-col gap-6">
          <section aria-label="Budget summary" className="card-hero animate-rise rounded-3xl p-5 shadow-card sm:p-6">
            <div className="flex flex-wrap items-end justify-between gap-4">
              <div>
                <p className="text-sm font-medium text-white/75">{summary.data.remaining < 0 ? 'Over budget by' : 'Left to spend'}</p>
                <p className="text-4xl font-extrabold tracking-tight tabular-nums">
                  <MoneyText amount={Math.abs(summary.data.remaining)} currency={currency} />
                </p>
              </div>
              <p className="text-sm text-white/75">
                <MoneyText amount={summary.data.actualExpenses} currency={currency} className="font-bold text-white" /> of{' '}
                <MoneyText amount={summary.data.spendableAmount} currency={currency} /> spent
              </p>
            </div>
            <div className="mt-4 h-2 overflow-hidden rounded-full bg-white/20">
              <div
                role="progressbar"
                aria-label="Budget used"
                aria-valuenow={Math.round(summary.data.percentUsed ?? 0)}
                aria-valuemin={0}
                aria-valuemax={100}
                className={cn('h-full origin-left animate-grow rounded-full', (summary.data.percentUsed ?? 0) > 100 ? 'bg-negative-600' : 'bg-brand-600')}
                style={{ width: `${Math.min(100, summary.data.percentUsed ?? 0)}%` }}
              />
            </div>
            <dl className="mt-5 grid grid-cols-1 gap-2 text-sm sm:grid-cols-3">
              {[
                ['Planned income', summary.data.plannedIncome],
                ['Savings target', summary.data.savingsTarget],
                ['Saved so far', summary.data.netCashFlow],
              ].map(([label, value]) => (
                <div key={label as string} className="flex min-w-0 items-center justify-between gap-2 rounded-2xl bg-white/10 px-3 py-2.5 ring-1 ring-white/10 sm:block sm:py-3">
                  <dt className="truncate text-[11px] font-semibold tracking-wide text-white/70 uppercase">{label}</dt>
                  <dd className="truncate font-bold tabular-nums sm:mt-1">{formatMoney(value as number, currency)}</dd>
                </div>
              ))}
            </dl>
            {summary.data.savingsProgressPercent !== null && (
              <p className="mt-3 text-xs text-white/75">
                {budget.data.savingsStrategy === 'PERCENTAGE' ? `Saving ${budget.data.savingsValue}% of income` : 'Fixed savings amount'} ·{' '}
                {summary.data.savingsProgressPercent.toFixed(0)}% of the target reached
              </p>
            )}
          </section>

          <div className="flex items-center justify-between gap-2 px-1">
            <h2 className="text-base font-semibold text-ink-900">Categories</h2>
            <div className="flex gap-1">
              <Button size="sm" variant="secondary" icon={<Icon name="edit" className="h-4 w-4" />} onClick={() => setMode('edit')}>
                Edit budget
              </Button>
              <Button size="sm" variant="ghost" onClick={() => setDeleting(true)} aria-label="Delete budget">
                <Icon name="trash" className="h-4 w-4" />
              </Button>
            </div>
          </div>

          {lines.length === 0 ? (
            <Card>
              <EmptyState icon="pie" title="No category limits" action={<Button size="sm" onClick={() => setMode('edit')}>Add limits</Button>}>
                Limits show how each category is tracking during the month.
              </EmptyState>
            </Card>
          ) : (
            <div className="grid grid-cols-1 gap-6 lg:grid-cols-5">
              <div className="lg:col-span-3">
                {grid}
                {summary.data.unbudgetedSpending > 0 && (
                  <p className="mt-3 px-1 text-sm text-ink-500">
                    <MoneyText amount={summary.data.unbudgetedSpending} currency={currency} className="font-semibold text-ink-700" /> spent in categories without a limit.
                  </p>
                )}
              </div>
              <div ref={detailRef} className="scroll-mt-20 lg:col-span-2">
                {selected ? (
                  <CategoryDetail line={selected} currency={currency} month={month} onEdit={() => setMode('edit')} />
                ) : (
                  <div className="hidden rounded-3xl border border-dashed border-line px-6 py-10 text-center text-sm text-ink-500 lg:block">
                    Select a category to see its details and recent transactions.
                  </div>
                )}
              </div>
            </div>
          )}
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
