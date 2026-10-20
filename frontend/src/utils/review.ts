import type { MonthlyReportResponse, TransactionResponse } from '../types/api';
import { fromMinor, toMinor } from './money';

/** The headline numbers of a period review. Amounts are in one currency. */
export interface PeriodReview {
  income: number;
  expenses: number;
  net: number;
  /** Share of income kept (net ÷ income), or null without income. */
  savingsRate: number | null;
  largest: { name: string; amount: number; color: string } | null;
  /** Change in spending against the previous period, in percent, or null if there was none before. */
  expenseChange: number | null;
  previousExpenses: number;
}

function percentChange(current: number, previous: number): number | null {
  return previous > 0 ? ((current - previous) / previous) * 100 : null;
}

export function reviewFromReport(report: MonthlyReportResponse): PeriodReview {
  const { income, expenses, net } = report.totals;
  const top = [...report.expensesByCategory].sort((a, b) => b.amount - a.amount)[0];
  return {
    income,
    expenses,
    net,
    savingsRate: income > 0 ? (net / income) * 100 : null,
    largest: top ? { name: top.category, amount: top.amount, color: top.color } : null,
    expenseChange: percentChange(expenses, report.previousMonth.expenses),
    previousExpenses: report.previousMonth.expenses,
  };
}

/**
 * A week's review from raw transactions: `current` and `previous` are the two 7-day windows.
 * Only the given currency counts and transfers are ignored, the same rules as the server's reports.
 */
export function reviewFromTransactions(
  current: TransactionResponse[],
  previous: TransactionResponse[],
  currency: string,
  colors: Map<number, string> = new Map(),
): PeriodReview {
  const totals = (list: TransactionResponse[]) => {
    let income = 0;
    let expenses = 0;
    const byCategory = new Map<string, { minor: number; color: string }>();
    for (const t of list) {
      if (t.currency !== currency) continue;
      const minor = toMinor(t.amount, currency);
      if (t.type === 'INCOME') income += minor;
      if (t.type === 'EXPENSE') {
        expenses += minor;
        const name = t.categoryName ?? 'Uncategorized';
        const entry = byCategory.get(name) ?? { minor: 0, color: (t.categoryId !== null && colors.get(t.categoryId)) || '#A9B6CC' };
        entry.minor += minor;
        byCategory.set(name, entry);
      }
    }
    return { income, expenses, byCategory };
  };
  const now = totals(current);
  const before = totals(previous);
  const top = [...now.byCategory.entries()].sort((a, b) => b[1].minor - a[1].minor)[0];
  const income = fromMinor(now.income, currency);
  const expenses = fromMinor(now.expenses, currency);
  const net = fromMinor(now.income - now.expenses, currency);
  const previousExpenses = fromMinor(before.expenses, currency);
  return {
    income,
    expenses,
    net,
    savingsRate: income > 0 ? (net / income) * 100 : null,
    largest: top ? { name: top[0], amount: fromMinor(top[1].minor, currency), color: top[1].color } : null,
    expenseChange: percentChange(expenses, previousExpenses),
    previousExpenses,
  };
}

/** Daily spending for a run of dates (oldest first), for the weekly bar chart. */
export function dailyTotals(transactions: TransactionResponse[], dates: string[], currency: string, locale?: string) {
  return dates.map((date) => {
    const day = transactions.filter((t) => t.transactionDate === date && t.currency === currency);
    const sum = (type: 'INCOME' | 'EXPENSE') => fromMinor(day.filter((t) => t.type === type).reduce((s, t) => s + toMinor(t.amount, currency), 0), currency);
    const [y, m, d] = date.split('-').map(Number);
    return { label: new Date(y!, m! - 1, d!).toLocaleDateString(locale, { weekday: 'short' }), income: sum('INCOME'), expenses: sum('EXPENSE'), net: 0 };
  });
}
