import { useState, type ReactNode } from 'react';
import { Link } from 'react-router';
import { InsightList } from '../components/InsightList';
import { TransactionList } from '../components/TransactionList';
import { Badge } from '../components/ui/Badge';
import { Card, cardLinkClass } from '../components/ui/Card';
import { CategoryIcon } from '../components/ui/CategoryIcon';
import { Icon } from '../components/ui/Icon';
import { MoneyText } from '../components/ui/MoneyText';
import { MonthPicker } from '../components/ui/MonthPicker';
import { ProgressBar } from '../components/ui/ProgressBar';
import { EmptyState, ErrorState, LoadingState } from '../components/ui/States';
import { useAsync } from '../hooks/useAsync';
import { useAuth } from '../hooks/useAuth';
import { budgetService } from '../services/budgetService';
import { categoryService } from '../services/categoryService';
import { dashboardService } from '../services/dashboardService';
import { insightService } from '../services/insightService';
import { transactionService } from '../services/transactionService';
import type { DashboardSummaryResponse } from '../types/api';
import { goalIcon, pastel } from '../utils/categoryStyle';
import { currentYearMonth, formatDate, monthRange } from '../utils/dates';
import { formatMoney } from '../utils/money';

export function greeting(now: Date = new Date()): string {
  const hour = now.getHours();
  return hour < 5 ? 'Good evening' : hour < 12 ? 'Good morning' : hour < 18 ? 'Good afternoon' : 'Good evening';
}

/** A figure inside the balance card. role=group gives each one an accessible name. */
function HeroStat({ label, icon, children, className }: { label: string; icon: 'trendUp' | 'trendDown' | 'pie'; children: ReactNode; className?: string }) {
  return (
    <div role="group" aria-label={label} className={`${className ?? ''} min-w-0 rounded-2xl bg-white/10 p-3 ring-1 ring-white/10`}>
      <p className="flex items-center gap-1.5 text-[11px] font-semibold tracking-wide text-white/75 uppercase">
        <Icon name={icon} className="h-3.5 w-3.5" />
        {label}
      </p>
      <p className="mt-1 truncate text-base font-bold tabular-nums sm:text-lg">{children}</p>
    </div>
  );
}

interface SpendLine {
  id: number;
  name: string;
  color: string;
  spent: number;
  limit: number | null;
  percent: number;
}

function SpendRow({ line, currency }: { line: SpendLine; currency: string }) {
  const over = line.limit !== null && line.spent > line.limit;
  return (
    <li className="flex items-center gap-3">
      <CategoryIcon name={line.name} color={line.color} />
      <div className="min-w-0 flex-1">
        <div className="mb-1.5 flex items-baseline justify-between gap-2 text-sm">
          <span className="truncate font-semibold text-ink-900">{line.name}</span>
          <span className="shrink-0 text-xs text-ink-500 tabular-nums">
            <span className={over ? 'font-semibold text-negative-600' : 'font-semibold text-ink-800'}>{formatMoney(line.spent, currency)}</span>
            {line.limit !== null && ` / ${formatMoney(line.limit, currency)}`}
          </span>
        </div>
        <ProgressBar
          value={line.percent}
          label={line.limit !== null ? `${line.name} spending` : `${line.name} share of spending`}
          color={over ? undefined : pastel(line.color)}
          tone="negative"
        />
      </div>
    </li>
  );
}

function BudgetSection({ s, month, currency }: { s: DashboardSummaryResponse; month: { year: number; month: number }; currency: string }) {
  const budgetId = s.budget?.budgetId;
  const lines = useAsync(async (): Promise<SpendLine[]> => {
    if (budgetId) {
      const summary = await budgetService.summary(budgetId);
      return summary.categories.map((c) => ({ id: c.categoryId, name: c.categoryName, color: c.color, spent: c.spent, limit: c.limit, percent: c.percentUsed }));
    }
    const spending = await dashboardService.spendingByCategory(month.year, month.month, currency);
    return spending.categories.map((c) => ({ id: c.categoryId, name: c.categoryName, color: c.color, spent: c.amount, limit: null, percent: c.percentOfTotal }));
  }, [budgetId, month.year, month.month, currency]);
  const top = [...(lines.data ?? [])].sort((a, b) => b.percent - a.percent).slice(0, 4);

  return (
    <Card title="Monthly budget" action={<Link to="/budget" className={cardLinkClass}>Open planner</Link>} className="lg:col-span-3">
      {s.budget ? (
        <div className="mb-5 flex flex-col gap-3">
          <div className="flex items-end justify-between gap-3">
            <div>
              <p className="text-xs font-semibold tracking-wide text-ink-500 uppercase">Left to spend</p>
              <p className="text-2xl font-bold tracking-tight text-ink-900">
                <MoneyText amount={s.budget.remaining} currency={s.budget.currency} signed={s.budget.remaining < 0} />
              </p>
            </div>
            <p className="text-right text-xs text-ink-500">
              <MoneyText amount={s.budget.spent} currency={s.budget.currency} className="font-semibold text-ink-800" /> of{' '}
              <MoneyText amount={s.budget.spendable} currency={s.budget.currency} />
            </p>
          </div>
          <ProgressBar
            value={s.budget.percentUsed ?? 0}
            label="Budget used"
            tone={(s.budget.percentUsed ?? 0) > 100 ? 'negative' : (s.budget.percentUsed ?? 0) >= 80 ? 'warning' : 'brand'}
            className="h-2"
          />
          <div className="flex flex-wrap gap-2">
            {s.budget.categoriesExceeded > 0 && <Badge tone="negative">{s.budget.categoriesExceeded} over limit</Badge>}
            {s.budget.categoriesNearLimit > 0 && <Badge tone="warning">{s.budget.categoriesNearLimit} near limit</Badge>}
            {s.budget.categoriesExceeded === 0 && s.budget.categoriesNearLimit === 0 && <Badge tone="positive">All categories on track</Badge>}
          </div>
        </div>
      ) : (
        <div className="mb-4 flex items-center justify-between gap-3 rounded-2xl bg-brand-50 px-4 py-3">
          <p className="text-sm text-ink-700">No budget for this month yet.</p>
          <Link to="/budget" className="shrink-0 text-sm font-bold text-brand-700 hover:underline">
            Create one
          </Link>
        </div>
      )}
      {lines.error ? (
        <ErrorState error={lines.error} onRetry={lines.reload} />
      ) : top.length > 0 ? (
        <ul className="flex flex-col gap-4" aria-label={s.budget ? 'Category limits' : 'Top spending'}>
          {top.map((line) => <SpendRow key={line.id} line={line} currency={currency} />)}
        </ul>
      ) : lines.loading ? (
        <LoadingState />
      ) : (
        <p className="text-sm text-ink-500">{s.budget ? 'Add category limits to see them here.' : 'Expenses you record will appear here by category.'}</p>
      )}
    </Card>
  );
}

function GoalsSection({ s }: { s: DashboardSummaryResponse }) {
  const [featured, ...others] = s.activeGoals;
  return (
    <section aria-labelledby="goals-heading" className="flex animate-rise flex-col gap-3 lg:col-span-2">
      <div className="flex items-center justify-between px-1">
        <h2 id="goals-heading" className="text-base font-semibold text-ink-900">Savings goals</h2>
        <Link to="/goals" className={cardLinkClass}>All goals</Link>
      </div>
      {!featured ? (
        <Card>
          <EmptyState icon="target" title="No active goals">
            {s.achievedGoalCount > 0 ? `You've achieved ${s.achievedGoalCount} goal${s.achievedGoalCount === 1 ? '' : 's'}. Set a new one!` : 'Give your savings a purpose.'}
          </EmptyState>
        </Card>
      ) : (
        <>
          <Link to="/goals" className="card-hero group block rounded-3xl p-5 shadow-card transition-transform hover:-translate-y-0.5">
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0">
                <p className="text-xs font-semibold tracking-wide text-white/75 uppercase">Featured goal</p>
                <h3 className="truncate text-lg font-bold">{featured.name}</h3>
              </div>
              <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-white/15">
                <Icon name={goalIcon(featured.name)} className="h-6 w-6" />
              </span>
            </div>
            <p className="mt-4 text-2xl font-bold tabular-nums">
              <MoneyText amount={featured.savedAmount} currency={featured.currency} />
              <span className="text-sm font-medium text-white/70"> of {formatMoney(featured.targetAmount, featured.currency)}</span>
            </p>
            <div className="mt-3 h-2 overflow-hidden rounded-full bg-white/20">
              <div
                role="progressbar"
                aria-label={`${featured.name} progress`}
                aria-valuenow={Math.round(featured.progressPercent)}
                aria-valuemin={0}
                aria-valuemax={100}
                className="h-full origin-left animate-grow rounded-full bg-positive-600"
                style={{ width: `${Math.min(100, featured.progressPercent)}%` }}
              />
            </div>
            <p className="mt-2 flex justify-between text-xs text-white/75">
              <span>{featured.progressPercent.toFixed(0)}% saved</span>
              {featured.targetDate && <span>by {formatDate(featured.targetDate)}</span>}
            </p>
          </Link>
          {others.length > 0 && (
            <Card bodyClassName="flex flex-col gap-4 p-4">
              {others.map((goal) => (
                <div key={goal.id} className="flex items-center gap-3">
                  <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-positive-50 text-positive-600" aria-hidden="true">
                    <Icon name={goalIcon(goal.name)} className="h-4.5 w-4.5" />
                  </span>
                  <div className="min-w-0 flex-1">
                    <div className="mb-1 flex justify-between gap-2 text-sm">
                      <span className="truncate font-semibold text-ink-900">{goal.name}</span>
                      <span className="text-ink-500">{goal.progressPercent.toFixed(0)}%</span>
                    </div>
                    <ProgressBar value={goal.progressPercent} label={`${goal.name} progress`} tone="positive" />
                  </div>
                </div>
              ))}
            </Card>
          )}
          {s.achievedGoalCount > 0 && <p className="px-1 text-xs font-semibold text-positive-600">+ {s.achievedGoalCount} achieved</p>}
        </>
      )}
    </section>
  );
}

export function DashboardPage() {
  const { user } = useAuth();
  const [month, setMonth] = useState(currentYearMonth);
  const currency = user?.preferredCurrency ?? 'CAD';

  const summary = useAsync(() => dashboardService.summary(month.year, month.month), [month.year, month.month]);
  const insights = useAsync(() => insightService.forMonth(month.year, month.month, currency), [month.year, month.month, currency]);
  const recent = useAsync(
    () => transactionService.search({ ...monthRange(month), sort: 'transactionDate,desc', page: 0, size: 5 }),
    [month.year, month.month],
  );
  const categories = useAsync(() => categoryService.list(), []);

  const s = summary.data;
  const flow = s?.cashFlow.find((c) => c.currency === currency);
  const balance = s?.balances.find((b) => b.currency === currency);
  const otherBalances = s?.balances.filter((b) => b.currency !== currency) ?? [];
  const today = new Date().toLocaleDateString(undefined, { weekday: 'long', month: 'long', day: 'numeric' });

  return (
    <>
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-sm font-medium text-ink-500">{today}</p>
          <h1 className="text-[1.75rem] leading-tight font-bold tracking-tight text-ink-900">
            {greeting()}, {user?.firstName ?? 'there'}
          </h1>
        </div>
        <MonthPicker value={month} onChange={setMonth} />
      </div>

      {summary.loading && !s ? (
        <LoadingState />
      ) : summary.error ? (
        <ErrorState error={summary.error} onRetry={summary.reload} />
      ) : s && s.balances.length === 0 ? (
        <Card>
          <EmptyState icon="wallet" title="Add your first account" action={<Link to="/accounts" className="text-sm font-bold text-brand-700 hover:underline">Go to accounts</Link>}>
            Nest works from the accounts you track: chequing, savings, cash, or credit cards.
          </EmptyState>
        </Card>
      ) : s ? (
        <div className="flex flex-col gap-6">
          <section aria-label="Monthly overview" className="card-hero animate-rise rounded-3xl p-5 shadow-card sm:p-6">
            <div role="group" aria-label="Total balance">
              <p className="text-sm font-medium text-white/75">Total balance</p>
              <p className="mt-1 text-4xl font-extrabold tracking-tight tabular-nums sm:text-5xl">
                <MoneyText amount={balance?.total ?? 0} currency={currency} />
              </p>
              <p className="mt-1 text-xs text-white/70">
                {balance?.accountCount ?? 0} open account{balance?.accountCount === 1 ? '' : 's'} in {currency}
              </p>
            </div>
            <div className="mt-5 grid grid-cols-2 gap-2 sm:grid-cols-3 sm:gap-3">
              <HeroStat label="Income" icon="trendUp">
                <MoneyText amount={flow?.income ?? 0} currency={currency} />
              </HeroStat>
              <HeroStat label="Expenses" icon="trendDown">
                <MoneyText amount={flow?.expenses ?? 0} currency={currency} />
              </HeroStat>
              <HeroStat label="Budget left" icon="pie" className="col-span-2 sm:col-span-1">
                {s.budget ? <MoneyText amount={s.budget.remaining} currency={s.budget.currency} /> : <span className="text-white/60">—</span>}
              </HeroStat>
            </div>
            <div role="group" aria-label="Net cash flow" className="mt-3 flex items-center justify-between rounded-2xl bg-black/15 px-3 py-2 text-sm">
              <span className="text-white/75">Net cash flow</span>
              <span className="font-bold tabular-nums">
                {(flow?.net ?? 0) > 0 && '+'}
                <MoneyText amount={flow?.net ?? 0} currency={currency} />
              </span>
            </div>
          </section>

          {otherBalances.length > 0 && (
            <p className="text-sm text-ink-500">
              Also held:{' '}
              {otherBalances.map((b, i) => (
                <span key={b.currency}>
                  {i > 0 && ', '}
                  <MoneyText amount={b.total} currency={b.currency} className="font-semibold text-ink-700" />
                </span>
              ))}{' '}
              (other currencies are never added together).
            </p>
          )}

          {insights.data && insights.data.insights.length > 0 && (
            <div className="flex flex-col gap-2">
              <InsightList insights={insights.data.insights} limit={1} />
              <Link to="/insights" className="self-end text-sm font-semibold text-brand-700 hover:underline">
                More insights
              </Link>
            </div>
          )}

          <div className="grid grid-cols-1 gap-6 lg:grid-cols-5">
            <BudgetSection s={s} month={month} currency={currency} />
            <GoalsSection s={s} />
          </div>

          <Card title="Recent transactions" action={<Link to="/transactions" className={cardLinkClass}>See all</Link>} bodyClassName="p-2 sm:p-3">
            {recent.error ? (
              <ErrorState error={recent.error} onRetry={recent.reload} />
            ) : recent.data && recent.data.content.length > 0 ? (
              <TransactionList transactions={recent.data.content} categories={categories.data} showAccount={false} />
            ) : recent.loading ? (
              <LoadingState />
            ) : (
              <EmptyState title="Nothing recorded this month">Tap + to add your first expense or income.</EmptyState>
            )}
          </Card>
        </div>
      ) : null}

      <Link
        to="/add"
        aria-label="Add expense"
        className="no-print fixed right-8 bottom-8 z-20 hidden h-14 w-14 items-center justify-center rounded-full bg-brand-600 text-on-brand shadow-glow transition-transform hover:scale-105 active:scale-95 lg:flex"
      >
        <Icon name="plus" className="h-7 w-7" />
      </Link>
    </>
  );
}
