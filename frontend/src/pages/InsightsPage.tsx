import { useState, type ReactNode } from 'react';
import { Link } from 'react-router';
import { InsightList } from '../components/InsightList';
import { SpendingDonut } from '../components/charts/SpendingDonut';
import { SpendingTrendLine } from '../components/charts/SpendingTrendLine';
import { TrendsChart } from '../components/charts/TrendsChart';
import { Card } from '../components/ui/Card';
import { CategoryIcon } from '../components/ui/CategoryIcon';
import { Icon } from '../components/ui/Icon';
import { MonthPicker } from '../components/ui/MonthPicker';
import { PageHeader } from '../components/ui/PageHeader';
import { SegmentedControl } from '../components/ui/SegmentedControl';
import { EmptyState, ErrorState, LoadingState } from '../components/ui/States';
import { useAsync } from '../hooks/useAsync';
import { useAuth } from '../hooks/useAuth';
import { categoryService } from '../services/categoryService';
import { dashboardService } from '../services/dashboardService';
import { insightService } from '../services/insightService';
import { reportService } from '../services/reportService';
import { toSpendingSlices, toTrendRows, type SpendingSlice, type TrendRow } from '../utils/chartData';
import { cn } from '../utils/cn';
import { currentYearMonth, formatDate, monthLabel, todayIso, type YearMonth } from '../utils/dates';
import { formatMoney } from '../utils/money';
import { dailyTotals, reviewFromReport, reviewFromTransactions, type PeriodReview } from '../utils/review';
import { fetchAllTransactions } from '../utils/transactions';

type Period = 'month' | 'week';

function shiftIso(iso: string, days: number): string {
  const [y, m, d] = iso.split('-').map(Number);
  const date = new Date(y!, m! - 1, d! + days);
  return todayIso(date);
}

function Stat({ label, children, hint, tone }: { label: string; children: ReactNode; hint?: ReactNode; tone?: 'positive' | 'negative' }) {
  return (
    <div role="group" aria-label={label} className="min-w-0 animate-rise rounded-3xl bg-surface p-4 shadow-card ring-1 ring-line/40">
      <p className="text-[11px] font-bold tracking-widest text-ink-400 uppercase">{label}</p>
      <p className={cn('mt-1 truncate text-xl font-bold tracking-tight tabular-nums', tone === 'positive' ? 'text-positive-600' : tone === 'negative' ? 'text-negative-600' : 'text-ink-900')}>
        {children}
      </p>
      {hint && <p className="mt-0.5 truncate text-xs text-ink-500">{hint}</p>}
    </div>
  );
}

function ReviewStats({ review, currency, previousLabel }: { review: PeriodReview; currency: string; previousLabel: string }) {
  const change = review.expenseChange;
  return (
    <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
      <Stat label="Total income" tone="positive">{formatMoney(review.income, currency)}</Stat>
      <Stat label="Total expenses">{formatMoney(review.expenses, currency)}</Stat>
      <Stat label="Savings rate" tone={review.savingsRate !== null && review.savingsRate < 0 ? 'negative' : undefined} hint={`${formatMoney(review.net, currency)} kept`}>
        {review.savingsRate === null ? '—' : `${review.savingsRate.toFixed(0)}%`}
      </Stat>
      <Stat
        label={`vs ${previousLabel}`}
        tone={change === null ? undefined : change > 0 ? 'negative' : 'positive'}
        hint={`Spent ${formatMoney(review.previousExpenses, currency)} before`}
      >
        {change === null ? '—' : (
          <span className="inline-flex items-center gap-1">
            <Icon name={change > 0 ? 'trendUp' : 'trendDown'} className="h-5 w-5" />
            {Math.abs(change).toFixed(0)}%
          </span>
        )}
      </Stat>
    </div>
  );
}

function LargestCategory({ review, currency }: { review: PeriodReview; currency: string }) {
  if (!review.largest) return null;
  const share = review.expenses > 0 ? (review.largest.amount / review.expenses) * 100 : 0;
  return (
    <div role="group" aria-label="Largest spending category" className="card-hero flex animate-rise items-center gap-4 rounded-3xl p-5 shadow-card">
      <CategoryIcon name={review.largest.name} color={review.largest.color} className="h-12 w-12 bg-white/15!" />
      <div className="min-w-0 flex-1">
        <p className="text-xs font-semibold tracking-wide text-white/75 uppercase">Largest spending category</p>
        <p className="truncate text-lg font-bold">{review.largest.name}</p>
      </div>
      <div className="text-right">
        <p className="text-lg font-bold tabular-nums">{formatMoney(review.largest.amount, currency)}</p>
        <p className="text-xs text-white/75">{share.toFixed(0)}% of spending</p>
      </div>
    </div>
  );
}

const reflections = [
  { value: 'yes', label: 'Yes, I did!', className: 'bg-positive-50 text-positive-600 ring-positive-600/40', selected: 'bg-positive-600 text-on-brand', reply: 'Wonderful. Staying on plan is how goals get reached — keep it going.' },
  { value: 'almost', label: 'Almost there', className: 'bg-accent-100 text-accent-600 ring-accent-600/40', selected: 'bg-accent-500 text-on-brand', reply: 'Close is progress. Check which category ran over and adjust its limit or habits.' },
  { value: 'improve', label: 'I need to improve', className: 'bg-brand-50 text-brand-700 ring-brand-600/40', selected: 'bg-brand-600 text-on-brand', reply: 'Honest reflection is the first step. Try one small change next month, like a weekly dining cap.' },
] as const;

/** A private check-in kept on this device only (it is a reflection, not financial data). */
function Reflection({ month, userId }: { month: YearMonth; userId?: number }) {
  const key = `nest.reflection.${userId ?? 'anon'}.${month.year}-${month.month}`;
  const read = () => {
    try {
      return localStorage.getItem(key);
    } catch {
      return null;
    }
  };
  const [answer, setAnswer] = useState<string | null>(read);
  const [shownKey, setShownKey] = useState(key);
  if (shownKey !== key) {
    setShownKey(key);
    setAnswer(read());
  }
  const choose = (value: string) => {
    setAnswer(value);
    try {
      localStorage.setItem(key, value);
    } catch {
      // Not saved; the answer still shows for this visit.
    }
  };
  const chosen = reflections.find((r) => r.value === answer);
  return (
    <Card title="Monthly check-in" description={`Did you stay within your budget in ${monthLabel(month)}?`}>
      <div role="radiogroup" aria-label="Did you stay within your budget this month?" className="grid grid-cols-1 gap-2 sm:grid-cols-3">
        {reflections.map((r) => (
          <button
            key={r.value}
            type="button"
            role="radio"
            aria-checked={answer === r.value}
            onClick={() => choose(r.value)}
            className={cn('rounded-2xl px-4 py-3 text-sm font-bold ring-1 transition-[transform,background-color] active:scale-95', answer === r.value ? r.selected : r.className)}
          >
            {r.label}
          </button>
        ))}
      </div>
      {chosen && (
        <p role="status" className="mt-3 flex animate-rise items-start gap-2 text-sm text-ink-600">
          <Icon name="sparkle" className="mt-0.5 h-4 w-4 shrink-0 text-brand-600" />
          {chosen.reply}
        </p>
      )}
    </Card>
  );
}

function MonthlyReview({ month, currency }: { month: YearMonth; currency: string }) {
  const report = useAsync(() => reportService.monthly(month.year, month.month, currency), [month.year, month.month, currency]);
  const trends = useAsync(() => dashboardService.monthlyTrends(month.year, month.month, 6, currency), [month.year, month.month, currency]);
  if (report.loading && !report.data) return <LoadingState />;
  if (report.error) return <ErrorState error={report.error} onRetry={report.reload} />;
  if (!report.data) return null;
  const review = reviewFromReport(report.data);
  const slices = toSpendingSlices({
    year: month.year,
    month: month.month,
    currency,
    total: report.data.totals.expenses,
    categories: report.data.expensesByCategory.map((c, i) => ({ categoryId: i, categoryName: c.category, color: c.color, amount: c.amount, percentOfTotal: c.percentOfTotal })),
  });
  const rows = trends.data ? toTrendRows(trends.data) : null;
  return <ReviewBody review={review} currency={currency} previousLabel="last month" slices={slices} trendRows={rows} trendError={trends.error ? <ErrorState error={trends.error} onRetry={trends.reload} /> : null} />;
}

function WeeklyReview({ currency }: { currency: string }) {
  const today = todayIso();
  const start = shiftIso(today, -6);
  const data = useAsync(async () => {
    const [current, previous, categories] = await Promise.all([
      fetchAllTransactions({ from: start, to: today }),
      fetchAllTransactions({ from: shiftIso(start, -7), to: shiftIso(start, -1) }),
      categoryService.list(),
    ]);
    return { current, previous, colors: new Map(categories.map((c) => [c.id, c.color])) };
  }, [start, today]);
  if (data.loading && !data.data) return <LoadingState />;
  if (data.error) return <ErrorState error={data.error} onRetry={data.reload} />;
  if (!data.data) return null;
  const review = reviewFromTransactions(data.data.current, data.data.previous, currency, data.data.colors);
  const dates = Array.from({ length: 7 }, (_, i) => shiftIso(start, i));
  const byCategory = new Map<string, { value: number; color: string }>();
  for (const t of data.data.current) {
    if (t.type !== 'EXPENSE' || t.currency !== currency) continue;
    const name = t.categoryName ?? 'Uncategorized';
    const entry = byCategory.get(name) ?? { value: 0, color: (t.categoryId !== null && data.data.colors.get(t.categoryId)) || '#A9B6CC' };
    entry.value = Math.round((entry.value + t.amount) * 100) / 100;
    byCategory.set(name, entry);
  }
  const slices: SpendingSlice[] = [...byCategory.entries()]
    .sort((a, b) => b[1].value - a[1].value)
    .map(([name, e]) => ({ name, value: e.value, color: e.color, percent: review.expenses > 0 ? (e.value / review.expenses) * 100 : 0 }));
  return (
    <ReviewBody
      review={review}
      currency={currency}
      previousLabel="last week"
      slices={slices}
      trendRows={dailyTotals(data.data.current, dates, currency)}
      subtitle={`${formatDate(start)} – ${formatDate(today)}`}
    />
  );
}

function ReviewBody({ review, currency, previousLabel, slices, trendRows, trendError, subtitle }: {
  review: PeriodReview;
  currency: string;
  previousLabel: string;
  slices: SpendingSlice[];
  trendRows: TrendRow[] | null;
  trendError?: ReactNode;
  subtitle?: string;
}) {
  const weekly = previousLabel === 'last week';
  return (
    <div className="flex flex-col gap-5">
      {subtitle && <p className="-mt-2 px-1 text-sm text-ink-500">{subtitle}</p>}
      <ReviewStats review={review} currency={currency} previousLabel={previousLabel} />
      <LargestCategory review={review} currency={currency} />
      <div className="grid grid-cols-1 gap-5 lg:grid-cols-2">
        <Card title="Where it went">
          {slices.length > 0 ? (
            <SpendingDonut slices={slices} currency={currency} total={review.expenses} />
          ) : (
            <EmptyState icon="pie" title="No spending yet">Expenses you record will appear here by category.</EmptyState>
          )}
        </Card>
        <Card title={weekly ? 'Daily spending' : 'Spending trend'} description={weekly ? 'The last 7 days' : 'The last 6 months'}>
          {trendRows ? <SpendingTrendLine rows={trendRows} currency={currency} /> : trendError ?? <LoadingState />}
        </Card>
      </div>
      {trendRows && (
        <Card title={weekly ? 'Income vs. expenses by day' : 'Monthly comparison'} description={`Income and expenses in ${currency}`}>
          <TrendsChart rows={trendRows} currency={currency} />
        </Card>
      )}
    </div>
  );
}

export function InsightsPage() {
  const { user } = useAuth();
  const currency = user?.preferredCurrency ?? 'CAD';
  const [month, setMonth] = useState(currentYearMonth);
  const [period, setPeriod] = useState<Period>('month');
  const insights = useAsync(() => insightService.forMonth(month.year, month.month, currency), [month.year, month.month, currency]);

  return (
    <>
      <PageHeader
        title="Insights"
        description="Your financial review, with the numbers behind each observation."
        actions={
          <>
            <SegmentedControl
              label="Period"
              value={period}
              onChange={setPeriod}
              className="w-48"
              options={[
                { value: 'week', label: 'Weekly' },
                { value: 'month', label: 'Monthly' },
              ]}
            />
            {period === 'month' && <MonthPicker value={month} onChange={setMonth} />}
          </>
        }
      />

      <div className="flex flex-col gap-6">
        {period === 'month' ? <MonthlyReview month={month} currency={currency} /> : <WeeklyReview currency={currency} />}

        <section aria-labelledby="observations" className="flex flex-col gap-3">
          <h2 id="observations" className="px-1 text-base font-semibold text-ink-900">
            Observations for {monthLabel(month)}
          </h2>
          {insights.loading && !insights.data ? (
            <LoadingState />
          ) : insights.error ? (
            <ErrorState error={insights.error} onRetry={insights.reload} />
          ) : insights.data ? (
            <>
              {insights.data.insights.length === 0 ? (
                <Card>
                  <EmptyState icon="bulb" title={`Nothing to flag for ${monthLabel(month)}`}>
                    As you record transactions, set budgets, and save toward goals, Nest will point out anything worth knowing.
                  </EmptyState>
                </Card>
              ) : (
                <InsightList insights={insights.data.insights} />
              )}
              <p className="px-1 text-xs text-ink-500">
                Based on data up to {formatDate(insights.data.asOf)} in {insights.data.currency}. {insights.data.disclaimer}
              </p>
            </>
          ) : null}
        </section>

        <Reflection month={month} userId={user?.id} />

        <Link
          to={`/reports?year=${month.year}&month=${month.month}`}
          className="flex h-13 items-center justify-center gap-2 rounded-2xl bg-brand-600 px-6 font-bold text-on-brand shadow-glow transition-transform active:scale-[0.97]"
        >
          <Icon name="file" className="h-5 w-5" />
          View full report
        </Link>
      </div>
    </>
  );
}
