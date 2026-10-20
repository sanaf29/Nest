import { useState } from 'react';
import { Link } from 'react-router';
import { SpendingDonut } from '../components/charts/SpendingDonut';
import { InsightList } from '../components/InsightList';
import { TrendsChart } from '../components/charts/TrendsChart';
import { Badge } from '../components/ui/Badge';
import { Card } from '../components/ui/Card';
import { MoneyText } from '../components/ui/MoneyText';
import { MonthPicker } from '../components/ui/MonthPicker';
import { PageHeader } from '../components/ui/PageHeader';
import { ProgressBar } from '../components/ui/ProgressBar';
import { EmptyState, ErrorState, LoadingState } from '../components/ui/States';
import { useAsync } from '../hooks/useAsync';
import { useAuth } from '../hooks/useAuth';
import { dashboardService } from '../services/dashboardService';
import { insightService } from '../services/insightService';
import { toSpendingSlices, toTrendRows } from '../utils/chartData';
import { currentYearMonth, formatDate, monthLabel } from '../utils/dates';

function StatCard({ label, children, hint }: { label: string; children: React.ReactNode; hint?: string }) {
  return (
    <div role="group" aria-label={label} className="rounded-2xl bg-white p-5 shadow-sm ring-1 ring-ink-200/70">
      <p className="text-sm text-ink-500">{label}</p>
      <div className="mt-1 text-2xl font-semibold tracking-tight text-ink-900">{children}</div>
      {hint && <p className="mt-1 text-xs text-ink-500">{hint}</p>}
    </div>
  );
}

export function DashboardPage() {
  const { user } = useAuth();
  const [month, setMonth] = useState(currentYearMonth);
  const currency = user?.preferredCurrency ?? 'CAD';

  const summary = useAsync(() => dashboardService.summary(month.year, month.month), [month.year, month.month]);
  const spending = useAsync(() => dashboardService.spendingByCategory(month.year, month.month, currency), [month.year, month.month, currency]);
  const trends = useAsync(() => dashboardService.monthlyTrends(month.year, month.month, 6, currency), [month.year, month.month, currency]);
  const insights = useAsync(() => insightService.forMonth(month.year, month.month, currency), [month.year, month.month, currency]);

  const s = summary.data;
  const flow = s?.cashFlow.find((c) => c.currency === currency);
  const balance = s?.balances.find((b) => b.currency === currency);
  const otherBalances = s?.balances.filter((b) => b.currency !== currency) ?? [];

  return (
    <>
      <PageHeader
        title={`Hi ${user?.firstName ?? 'there'}`}
        description={`Here is your money in ${monthLabel(month)}.`}
        actions={<MonthPicker value={month} onChange={setMonth} />}
      />

      {summary.loading && !s ? (
        <LoadingState />
      ) : summary.error ? (
        <ErrorState error={summary.error} onRetry={summary.reload} />
      ) : s && s.balances.length === 0 ? (
        <Card>
          <EmptyState icon="wallet" title="Add your first account" action={<Link to="/accounts" className="text-sm font-medium text-brand-700 hover:underline">Go to accounts</Link>}>
            Nest works from the accounts you track: chequing, savings, cash, or credit cards.
          </EmptyState>
        </Card>
      ) : s ? (
        <div className="flex flex-col gap-6">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <StatCard label="Total balance" hint={`${balance?.accountCount ?? 0} open account${balance?.accountCount === 1 ? '' : 's'} in ${currency}`}>
              <MoneyText amount={balance?.total ?? 0} currency={currency} />
            </StatCard>
            <StatCard label="Income">
              <MoneyText amount={flow?.income ?? 0} currency={currency} className="text-positive-600" />
            </StatCard>
            <StatCard label="Expenses">
              <MoneyText amount={flow?.expenses ?? 0} currency={currency} />
            </StatCard>
            <StatCard label="Net cash flow" hint="Income minus expenses; transfers excluded">
              <MoneyText amount={flow?.net ?? 0} currency={currency} signed />
            </StatCard>
          </div>

          {insights.data && insights.data.insights.length > 0 && (
            <Card title="Insights" action={<Link to="/insights" className="text-sm font-medium text-brand-700 hover:underline">See all</Link>}>
              <InsightList insights={insights.data.insights} limit={2} />
            </Card>
          )}

          {otherBalances.length > 0 && (
            <p className="text-sm text-ink-500">
              Also held:{' '}
              {otherBalances.map((b, i) => (
                <span key={b.currency}>
                  {i > 0 && ', '}
                  <MoneyText amount={b.total} currency={b.currency} className="font-medium text-ink-700" />
                </span>
              ))}{' '}
              (other currencies are never added together).
            </p>
          )}

          <div className="grid grid-cols-1 gap-6 lg:grid-cols-5">
            <Card title="Spending by category" className="lg:col-span-3">
              {spending.loading && !spending.data ? (
                <LoadingState />
              ) : spending.error ? (
                <ErrorState error={spending.error} onRetry={spending.reload} />
              ) : spending.data && spending.data.categories.length > 0 ? (
                <SpendingDonut slices={toSpendingSlices(spending.data)} currency={currency} total={spending.data.total} />
              ) : (
                <EmptyState icon="pie" title="No spending this month">Expenses you record will appear here by category.</EmptyState>
              )}
            </Card>

            <Card
              title="Monthly budget"
              className="lg:col-span-2"
              action={<Link to="/budget" className="text-sm font-medium text-brand-700 hover:underline">Open planner</Link>}
            >
              {s.budget ? (
                <div className="flex flex-col gap-4">
                  <div>
                    <p className="text-sm text-ink-500">Remaining to spend</p>
                    <p className="text-3xl font-semibold tracking-tight">
                      <MoneyText amount={s.budget.remaining} currency={s.budget.currency} signed={s.budget.remaining < 0} />
                    </p>
                  </div>
                  <ProgressBar
                    value={s.budget.percentUsed ?? 0}
                    label="Budget used"
                    tone={(s.budget.percentUsed ?? 0) > 100 ? 'negative' : (s.budget.percentUsed ?? 0) >= 80 ? 'warning' : 'brand'}
                  />
                  <p className="text-sm text-ink-600">
                    <MoneyText amount={s.budget.spent} currency={s.budget.currency} /> of{' '}
                    <MoneyText amount={s.budget.spendable} currency={s.budget.currency} /> spent
                  </p>
                  <div className="flex flex-wrap gap-2">
                    {s.budget.categoriesExceeded > 0 && <Badge tone="negative">{s.budget.categoriesExceeded} over limit</Badge>}
                    {s.budget.categoriesNearLimit > 0 && <Badge tone="warning">{s.budget.categoriesNearLimit} near limit</Badge>}
                    {s.budget.categoriesExceeded === 0 && s.budget.categoriesNearLimit === 0 && <Badge tone="positive">All categories on track</Badge>}
                  </div>
                </div>
              ) : (
                <EmptyState icon="pie" title="No budget for this month" action={<Link to="/budget" className="text-sm font-medium text-brand-700 hover:underline">Create one</Link>}>
                  Plan your income, savings, and category limits.
                </EmptyState>
              )}
            </Card>
          </div>

          <div className="grid grid-cols-1 gap-6 lg:grid-cols-5">
            <Card title="Last 6 months" description={`Income vs. expenses in ${currency}`} className="lg:col-span-3">
              {trends.data ? <TrendsChart rows={toTrendRows(trends.data)} currency={currency} /> : trends.error ? <ErrorState error={trends.error} onRetry={trends.reload} /> : <LoadingState />}
            </Card>

            <Card
              title="Savings goals"
              className="lg:col-span-2"
              action={<Link to="/goals" className="text-sm font-medium text-brand-700 hover:underline">All goals</Link>}
            >
              {s.activeGoals.length === 0 ? (
                <EmptyState icon="target" title="No active goals">
                  {s.achievedGoalCount > 0 ? `You've achieved ${s.achievedGoalCount} goal${s.achievedGoalCount === 1 ? '' : 's'}. Set a new one!` : 'Give your savings a purpose.'}
                </EmptyState>
              ) : (
                <ul className="flex flex-col gap-4">
                  {s.activeGoals.map((goal) => (
                    <li key={goal.id}>
                      <div className="mb-1 flex items-baseline justify-between gap-2 text-sm">
                        <span className="truncate font-medium text-ink-800">{goal.name}</span>
                        <span className="shrink-0 text-ink-500">{goal.progressPercent.toFixed(0)}%</span>
                      </div>
                      <ProgressBar value={goal.progressPercent} label={`${goal.name} progress`} tone="positive" />
                      <p className="mt-1 text-xs text-ink-500">
                        <MoneyText amount={goal.savedAmount} currency={goal.currency} /> of <MoneyText amount={goal.targetAmount} currency={goal.currency} />
                        {goal.targetDate && ` · by ${formatDate(goal.targetDate)}`}
                      </p>
                    </li>
                  ))}
                  {s.achievedGoalCount > 0 && <li className="text-xs text-positive-600">+ {s.achievedGoalCount} achieved</li>}
                </ul>
              )}
            </Card>
          </div>
        </div>
      ) : null}
    </>
  );
}
