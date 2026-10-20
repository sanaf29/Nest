import { useMemo, useState } from 'react';
import { TransactionList } from '../components/TransactionList';
import { Card } from '../components/ui/Card';
import { MoneyText } from '../components/ui/MoneyText';
import { MonthPicker } from '../components/ui/MonthPicker';
import { PageHeader } from '../components/ui/PageHeader';
import { EmptyState, ErrorState, LoadingState } from '../components/ui/States';
import { useAsync } from '../hooks/useAsync';
import { useAuth } from '../hooks/useAuth';
import { calendarWeeks, currentYearMonth, formatDate, monthLabel, monthRange, todayIso } from '../utils/dates';
import { formatMoney } from '../utils/money';
import { fetchAllTransactions, groupByDate } from '../utils/transactions';
import { cn } from '../utils/cn';

const WEEKDAYS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

export function CalendarPage() {
  const { user } = useAuth();
  const currency = user?.preferredCurrency ?? 'CAD';
  const [month, setMonth] = useState(currentYearMonth);
  const [selected, setSelected] = useState<string | null>(null);
  const transactions = useAsync(() => fetchAllTransactions(monthRange(month)), [month.year, month.month]);
  const days = useMemo(() => groupByDate(transactions.data ?? [], currency), [transactions.data, currency]);
  const today = todayIso();
  const selectedDay = selected ? days.get(selected) : undefined;

  return (
    <>
      <PageHeader
        title="Calendar"
        description={`Daily income and expenses in ${currency}. Select a day to see its transactions.`}
        actions={
          <MonthPicker
            value={month}
            onChange={(m) => {
              setSelected(null);
              setMonth(m);
            }}
          />
        }
      />
      {transactions.error ? (
        <ErrorState error={transactions.error} onRetry={transactions.reload} />
      ) : (
        <div className="grid grid-cols-1 gap-6 xl:grid-cols-3">
          <Card className="xl:col-span-2" bodyClassName="p-2 sm:p-4">
            <table className="w-full table-fixed border-separate border-spacing-1" aria-label={monthLabel(month)}>
              <thead>
                <tr>
                  {WEEKDAYS.map((d) => (
                    <th key={d} scope="col" className="pb-1 text-xs font-medium text-ink-500">
                      {d}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {calendarWeeks(month).map((week, i) => (
                  <tr key={i}>
                    {week.map((date, j) => {
                      if (!date) return <td key={j} />;
                      const day = days.get(date);
                      const label = `${formatDate(date)}${day ? `: income ${formatMoney(day.income, currency)}, expenses ${formatMoney(day.expenses, currency)}` : ', no transactions'}`;
                      return (
                        <td key={date} className="h-16 align-top sm:h-20">
                          <button
                            type="button"
                            onClick={() => setSelected(date)}
                            aria-label={label}
                            aria-pressed={selected === date}
                            className={cn(
                              'flex h-full w-full flex-col items-start gap-0.5 rounded-lg p-1.5 text-left text-xs transition-colors',
                              selected === date ? 'bg-brand-50 ring-2 ring-brand-600' : 'hover:bg-ink-50',
                              day && 'bg-ink-50/60',
                            )}
                          >
                            <span className={cn('font-medium', date === today ? 'rounded-full bg-brand-700 px-1.5 text-white' : 'text-ink-700')}>{Number(date.slice(8))}</span>
                            {day && day.income > 0 && <span className="hidden truncate text-positive-600 sm:block">+{formatMoney(day.income, currency)}</span>}
                            {day && day.expenses > 0 && <span className="hidden truncate text-negative-600 sm:block">−{formatMoney(day.expenses, currency)}</span>}
                            {day && <span className="mt-auto h-1.5 w-1.5 rounded-full bg-brand-600 sm:hidden" aria-hidden="true" />}
                          </button>
                        </td>
                      );
                    })}
                  </tr>
                ))}
              </tbody>
            </table>
            {transactions.loading && <LoadingState label="Loading month…" />}
          </Card>

          <Card title={selected ? formatDate(selected) : 'Select a day'} bodyClassName="p-0">
            {!selected ? (
              <EmptyState icon="calendar" title="Pick a date">Tap a day in the calendar to see what happened.</EmptyState>
            ) : selectedDay ? (
              <>
                <div className="flex gap-4 border-b border-ink-100 px-5 py-3 text-sm">
                  <span>
                    In <MoneyText amount={selectedDay.income} currency={currency} className="font-medium text-positive-600" />
                  </span>
                  <span>
                    Out <MoneyText amount={selectedDay.expenses} currency={currency} className="font-medium text-negative-600" />
                  </span>
                </div>
                <TransactionList transactions={selectedDay.transactions} />
              </>
            ) : (
              <EmptyState title="Nothing on this day" />
            )}
          </Card>
        </div>
      )}
    </>
  );
}
