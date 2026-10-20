import { useState } from 'react';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { Icon } from '../components/ui/Icon';
import { MoneyText } from '../components/ui/MoneyText';
import { MonthPicker } from '../components/ui/MonthPicker';
import { PageHeader } from '../components/ui/PageHeader';
import { ErrorState, FormError, LoadingState } from '../components/ui/States';
import { useAsync } from '../hooks/useAsync';
import { useAuth } from '../hooks/useAuth';
import { reportService } from '../services/reportService';
import { currentYearMonth, formatDate, monthLabel, monthRange } from '../utils/dates';
import { toApiError } from '../utils/errors';

function Delta({ current, previous, currency, lowerIsBetter = false }: { current: number; previous: number; currency: string; lowerIsBetter?: boolean }) {
  const diff = Math.round((current - previous) * 100) / 100;
  if (diff === 0) return <span className="text-xs text-ink-500">same as last month</span>;
  const good = lowerIsBetter ? diff < 0 : diff > 0;
  return (
    <span className={good ? 'text-xs text-positive-600' : 'text-xs text-negative-600'}>
      {diff > 0 ? '▲' : '▼'} <MoneyText amount={Math.abs(diff)} currency={currency} /> vs last month
    </span>
  );
}

export function ReportsPage() {
  const { user } = useAuth();
  const currency = user?.preferredCurrency ?? 'CAD';
  const [month, setMonth] = useState(currentYearMonth);
  const report = useAsync(() => reportService.monthly(month.year, month.month, currency), [month.year, month.month, currency]);
  const [busy, setBusy] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function download(kind: 'report' | 'transactions') {
    setBusy(kind);
    setError(null);
    try {
      if (kind === 'report') await reportService.downloadMonthlyCsv(month.year, month.month, currency);
      else await reportService.downloadTransactionsCsv(monthRange(month));
    } catch (e) {
      setError(toApiError(e).message);
    } finally {
      setBusy(null);
    }
  }

  const r = report.data;
  return (
    <>
      <PageHeader
        title={`Report · ${monthLabel(month)}`}
        description={`All amounts in ${currency}. Transfers between your accounts are not counted.`}
        actions={
          <div className="no-print flex flex-wrap gap-2">
            <MonthPicker value={month} onChange={setMonth} />
            <Button variant="secondary" icon={<Icon name="download" className="h-4 w-4" />} loading={busy === 'report'} onClick={() => download('report')}>Report CSV</Button>
            <Button variant="secondary" icon={<Icon name="download" className="h-4 w-4" />} loading={busy === 'transactions'} onClick={() => download('transactions')}>Transactions CSV</Button>
            <Button variant="secondary" icon={<Icon name="file" className="h-4 w-4" />} onClick={() => window.print()}>Print</Button>
          </div>
        }
      />
      <div className="no-print mb-4"><FormError message={error} /></div>

      {report.loading && !r ? (
        <LoadingState />
      ) : report.error ? (
        <ErrorState error={report.error} onRetry={report.reload} />
      ) : r ? (
        <div className="flex flex-col gap-6">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
            {([['Income', r.totals.income, r.previousMonth.income, false], ['Expenses', r.totals.expenses, r.previousMonth.expenses, true], ['Net cash flow', r.totals.net, r.previousMonth.net, false]] as const).map(([label, value, prev, lower]) => (
              <div key={label} className="rounded-2xl bg-white p-5 shadow-sm ring-1 ring-ink-200/70">
                <p className="text-sm text-ink-500">{label}</p>
                <p className="mt-1 text-2xl font-semibold"><MoneyText amount={value} currency={currency} signed={label === 'Net cash flow'} /></p>
                <Delta current={value} previous={prev} currency={currency} lowerIsBetter={lower} />
              </div>
            ))}
          </div>

          <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
            {([['Expenses by category', r.expensesByCategory], ['Income by category', r.incomeByCategory]] as const).map(([title, rows]) => (
              <Card key={title} title={title} bodyClassName="p-0">
                {rows.length === 0 ? <p className="p-5 text-sm text-ink-500">None this month.</p> : (
                  <table className="w-full text-sm">
                    <thead className="text-left text-xs text-ink-500"><tr><th scope="col" className="px-5 py-2 font-medium">Category</th><th scope="col" className="px-5 py-2 text-right font-medium">Amount</th><th scope="col" className="px-5 py-2 text-right font-medium">Share</th></tr></thead>
                    <tbody className="divide-y divide-ink-100">
                      {rows.map((c) => (
                        <tr key={c.category}>
                          <td className="px-5 py-2"><span className="mr-2 inline-block h-2.5 w-2.5 rounded-full" style={{ backgroundColor: c.color }} aria-hidden="true" />{c.category}</td>
                          <td className="px-5 py-2 text-right"><MoneyText amount={c.amount} currency={currency} /></td>
                          <td className="px-5 py-2 text-right text-ink-500">{c.percentOfTotal.toFixed(1)}%</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </Card>
            ))}
          </div>

          <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
            <Card title="Budget" bodyClassName="p-0">
              {r.budget ? (
                <table className="w-full text-sm">
                  <thead className="text-left text-xs text-ink-500"><tr><th scope="col" className="px-5 py-2 font-medium">Category</th><th scope="col" className="px-5 py-2 text-right font-medium">Spent / limit</th><th scope="col" className="px-5 py-2 text-right font-medium">Status</th></tr></thead>
                  <tbody className="divide-y divide-ink-100">
                    {r.budget.categories.map((l) => (
                      <tr key={l.category}>
                        <td className="px-5 py-2">{l.category}</td>
                        <td className="px-5 py-2 text-right"><MoneyText amount={l.spent} currency={currency} /> / <MoneyText amount={l.limit} currency={currency} /></td>
                        <td className="px-5 py-2 text-right"><Badge tone={l.status === 'EXCEEDED' ? 'negative' : l.status === 'NEAR_LIMIT' ? 'warning' : 'positive'}>{l.percentUsed.toFixed(0)}%</Badge></td>
                      </tr>
                    ))}
                    <tr className="font-medium"><td className="px-5 py-2">Remaining</td><td className="px-5 py-2 text-right" colSpan={2}><MoneyText amount={r.budget.remaining} currency={currency} signed={r.budget.remaining < 0} /></td></tr>
                  </tbody>
                </table>
              ) : <p className="p-5 text-sm text-ink-500">No budget for this month.</p>}
            </Card>
            <Card title="Largest expenses" bodyClassName="p-0">
              {r.topExpenses.length === 0 ? <p className="p-5 text-sm text-ink-500">None this month.</p> : (
                <ul className="divide-y divide-ink-100 text-sm">
                  {r.topExpenses.map((t, i) => (
                    <li key={i} className="flex justify-between gap-3 px-5 py-2">
                      <span className="min-w-0 truncate">{t.description ?? t.category} <span className="text-xs text-ink-500">· {formatDate(t.date)} · {t.account}</span></span>
                      <MoneyText amount={t.amount} currency={currency} className="font-medium" />
                    </li>
                  ))}
                </ul>
              )}
            </Card>
          </div>

          {r.goals.length > 0 && (
            <Card title="Savings goals" bodyClassName="p-0">
              <ul className="divide-y divide-ink-100 text-sm">
                {r.goals.map((g) => (
                  <li key={g.name} className="flex justify-between gap-3 px-5 py-2">
                    <span>{g.name} {g.status === 'ACHIEVED' && <Badge tone="positive">achieved</Badge>}</span>
                    <span><MoneyText amount={g.saved} currency={g.currency} /> of <MoneyText amount={g.target} currency={g.currency} /> ({g.progressPercent.toFixed(0)}%)</span>
                  </li>
                ))}
              </ul>
            </Card>
          )}
        </div>
      ) : null}
    </>
  );
}
