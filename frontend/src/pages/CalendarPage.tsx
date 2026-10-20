import { useMemo, useState } from 'react';
import { Link } from 'react-router';
import { TransactionList } from '../components/TransactionList';
import { Card } from '../components/ui/Card';
import { CategoryIcon } from '../components/ui/CategoryIcon';
import { Icon } from '../components/ui/Icon';
import { MoneyText } from '../components/ui/MoneyText';
import { MonthPicker } from '../components/ui/MonthPicker';
import { PageHeader } from '../components/ui/PageHeader';
import { EmptyState, ErrorState, LoadingState } from '../components/ui/States';
import { useAsync } from '../hooks/useAsync';
import { useAuth } from '../hooks/useAuth';
import { recurringService } from '../services/recurringService';
import { reminderService } from '../services/reminderService';
import type { RecurringResponse, ReminderResponse } from '../types/api';
import { calendarWeeks, currentYearMonth, formatDate, monthLabel, monthRange, todayIso } from '../utils/dates';
import { formatMoney } from '../utils/money';
import { fetchAllTransactions, groupByDate } from '../utils/transactions';
import { cn } from '../utils/cn';

const WEEKDAYS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

/** Something planned for a day: a recurring payday, bill, or subscription that hasn't happened yet. */
interface ScheduledItem {
  rule: RecurringResponse;
  kind: 'Payday' | 'Subscription' | 'Bill';
}

function scheduledKind(rule: RecurringResponse): ScheduledItem['kind'] {
  if (rule.type === 'INCOME') return 'Payday';
  return /subscri|stream|netflix|spotify|membership/i.test(`${rule.categoryName} ${rule.description}`) ? 'Subscription' : 'Bill';
}

const dot = {
  income: 'bg-positive-600',
  expense: 'bg-brand-600',
  scheduled: 'bg-warning-600',
  reminder: 'bg-accent-500',
};

export function CalendarPage() {
  const { user } = useAuth();
  const currency = user?.preferredCurrency ?? 'CAD';
  const [month, setMonth] = useState(currentYearMonth);
  const [selected, setSelected] = useState<string | null>(null);
  const transactions = useAsync(() => fetchAllTransactions(monthRange(month)), [month.year, month.month]);
  // Plans and reminders are extras: if they fail to load, the calendar still shows transactions.
  const recurring = useAsync(() => recurringService.list(), []);
  const reminders = useAsync(() => reminderService.list(), []);
  const days = useMemo(() => groupByDate(transactions.data ?? [], currency), [transactions.data, currency]);
  const today = todayIso();

  const scheduled = useMemo(() => {
    const byDate = new Map<string, ScheduledItem[]>();
    const { from, to } = monthRange(month);
    for (const rule of recurring.data ?? []) {
      if (rule.status !== 'ACTIVE') continue;
      for (const date of rule.upcoming) {
        // Only future plans: past occurrences are already real transactions.
        if (date < from || date > to || date < today) continue;
        byDate.set(date, [...(byDate.get(date) ?? []), { rule, kind: scheduledKind(rule) }]);
      }
    }
    return byDate;
  }, [recurring.data, month, today]);

  const remindersByDate = useMemo(() => {
    const byDate = new Map<string, ReminderResponse[]>();
    for (const reminder of reminders.data ?? []) {
      if (reminder.status !== 'SCHEDULED' || !reminder.dueAt) continue;
      const date = todayIso(new Date(reminder.dueAt));
      byDate.set(date, [...(byDate.get(date) ?? []), reminder]);
    }
    return byDate;
  }, [reminders.data]);

  const selectedDay = selected ? days.get(selected) : undefined;
  const selectedPlans = selected ? scheduled.get(selected) ?? [] : [];
  const selectedReminders = selected ? remindersByDate.get(selected) ?? [] : [];
  const selectedEmpty = !selectedDay && selectedPlans.length === 0 && selectedReminders.length === 0;

  return (
    <>
      <PageHeader
        title="Calendar"
        description={`Money in and out, upcoming bills, paydays, and reminders, in ${currency}.`}
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
          <Card className="xl:col-span-2" bodyClassName="p-3 sm:p-5">
            <table className="w-full table-fixed border-separate border-spacing-1" aria-label={monthLabel(month)}>
              <thead>
                <tr>
                  {WEEKDAYS.map((d) => (
                    <th key={d} scope="col" className="pb-2 text-[11px] font-bold tracking-wide text-ink-400 uppercase">
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
                      const plans = scheduled.get(date) ?? [];
                      const notes = remindersByDate.get(date) ?? [];
                      const extras = [plans.length > 0 && `${plans.length} scheduled`, notes.length > 0 && `${notes.length} reminder${notes.length === 1 ? '' : 's'}`].filter(Boolean);
                      const label = `${formatDate(date)}${day ? `: income ${formatMoney(day.income, currency)}, expenses ${formatMoney(day.expenses, currency)}` : ', no transactions'}${extras.length ? `, ${extras.join(', ')}` : ''}`;
                      const isToday = date === today;
                      return (
                        <td key={date} className="h-14 align-top sm:h-20">
                          <button
                            type="button"
                            onClick={() => setSelected(date)}
                            aria-label={label}
                            aria-pressed={selected === date}
                            className={cn(
                              'flex h-full w-full flex-col items-center gap-1 rounded-2xl p-1 text-xs transition-colors sm:items-start sm:p-2',
                              selected === date ? 'bg-brand-50 ring-2 ring-brand-500' : day || plans.length || notes.length ? 'bg-surface-2 hover:bg-ink-100' : 'hover:bg-ink-100/60',
                            )}
                          >
                            <span
                              className={cn(
                                'flex h-6 min-w-6 items-center justify-center rounded-full px-1 font-semibold',
                                isToday ? 'bg-brand-600 text-on-brand' : 'text-ink-700',
                              )}
                            >
                              {Number(date.slice(8))}
                            </span>
                            {day && day.income > 0 && <span className="hidden max-w-full truncate font-medium text-positive-600 sm:block">+{formatMoney(day.income, currency)}</span>}
                            {day && day.expenses > 0 && <span className="hidden max-w-full truncate font-medium text-negative-600 sm:block">−{formatMoney(day.expenses, currency)}</span>}
                            <span className="mt-auto flex gap-0.5" aria-hidden="true">
                              {day && day.income > 0 && <span className={cn('h-1.5 w-1.5 rounded-full', dot.income)} />}
                              {day && day.expenses > 0 && <span className={cn('h-1.5 w-1.5 rounded-full sm:hidden', dot.expense)} />}
                              {plans.length > 0 && <span className={cn('h-1.5 w-1.5 rounded-full', dot.scheduled)} />}
                              {notes.length > 0 && <span className={cn('h-1.5 w-1.5 rounded-full', dot.reminder)} />}
                            </span>
                          </button>
                        </td>
                      );
                    })}
                  </tr>
                ))}
              </tbody>
            </table>
            <ul className="mt-3 flex flex-wrap gap-x-4 gap-y-1 px-1 text-xs text-ink-500" aria-label="Legend">
              {[
                [dot.income, 'Income'],
                [dot.expense, 'Expenses'],
                [dot.scheduled, 'Bills, paydays & subscriptions'],
                [dot.reminder, 'Reminders'],
              ].map(([cls, text]) => (
                <li key={text} className="flex items-center gap-1.5">
                  <span className={cn('h-2 w-2 rounded-full', cls)} aria-hidden="true" />
                  {text}
                </li>
              ))}
            </ul>
            {transactions.loading && <LoadingState label="Loading month…" />}
          </Card>

          <Card title={selected ? formatDate(selected) : 'Select a day'} bodyClassName="p-0">
            {!selected ? (
              <EmptyState icon="calendar" title="Pick a date">Tap a day to see its transactions, upcoming bills, and reminders.</EmptyState>
            ) : selectedEmpty ? (
              <EmptyState title="Nothing on this day" />
            ) : (
              <div className="flex flex-col gap-4 pb-3">
                {selectedDay && (
                  <div className="px-2 sm:px-3">
                    <div className="mx-3 mt-3 mb-1 flex gap-4 text-sm">
                      <span className="text-ink-500">
                        In <MoneyText amount={selectedDay.income} currency={currency} className="font-bold text-positive-600" />
                      </span>
                      <span className="text-ink-500">
                        Out <MoneyText amount={selectedDay.expenses} currency={currency} className="font-bold text-negative-600" />
                      </span>
                    </div>
                    <TransactionList transactions={selectedDay.transactions} />
                  </div>
                )}
                {selectedPlans.length > 0 && (
                  <div className="px-5">
                    <h3 className="mb-2 text-[11px] font-bold tracking-widest text-ink-400 uppercase">Coming up</h3>
                    <ul className="flex flex-col gap-2">
                      {selectedPlans.map(({ rule, kind }) => (
                        <li key={rule.id} className="flex items-center gap-3 rounded-2xl bg-surface-2 p-3 ring-1 ring-line/40">
                          <CategoryIcon name={rule.categoryName} color={kind === 'Payday' ? '#83C9A5' : '#F2C27B'} className="h-9 w-9 rounded-xl" />
                          <div className="min-w-0 flex-1">
                            <p className="truncate text-sm font-semibold text-ink-900">{rule.description}</p>
                            <p className="text-xs text-ink-500">{kind} · {rule.accountName}</p>
                          </div>
                          <MoneyText amount={rule.type === 'INCOME' ? rule.amount : -rule.amount} currency={rule.currency} signed className="text-sm font-bold" />
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
                {selectedReminders.length > 0 && (
                  <div className="px-5">
                    <h3 className="mb-2 text-[11px] font-bold tracking-widest text-ink-400 uppercase">Reminders</h3>
                    <ul className="flex flex-col gap-2">
                      {selectedReminders.map((r) => (
                        <li key={r.id} className="flex items-center gap-3 rounded-2xl bg-accent-100 p-3">
                          <Icon name="bell" className="h-5 w-5 shrink-0 text-accent-600" />
                          <div className="min-w-0 flex-1">
                            <p className="truncate text-sm font-semibold text-ink-900">{r.title}</p>
                            <p className="text-xs text-ink-500">
                              {new Date(r.dueAt!).toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' })}
                              {r.location && ` · ${r.location}`}
                            </p>
                          </div>
                        </li>
                      ))}
                    </ul>
                    <Link to="/reminders" className="mt-2 inline-block text-xs font-semibold text-brand-700 hover:underline">
                      Manage reminders
                    </Link>
                  </div>
                )}
              </div>
            )}
          </Card>
        </div>
      )}
    </>
  );
}
