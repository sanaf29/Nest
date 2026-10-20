import type { CategorySpendingResponse, MonthlyTrendsResponse } from '../types/api';

export interface TrendRow {
  label: string;
  income: number;
  expenses: number;
  net: number;
}

/** API months → chart rows with short labels ("Oct 26"), oldest first. */
export function toTrendRows(trends: MonthlyTrendsResponse, locale?: string): TrendRow[] {
  return trends.months.map((m) => ({
    label: new Date(m.year, m.month - 1, 1).toLocaleDateString(locale, { month: 'short', year: '2-digit' }),
    income: m.income,
    expenses: m.expenses,
    net: m.net,
  }));
}

export interface SpendingSlice {
  name: string;
  value: number;
  color: string;
  percent: number;
}

/**
 * Category spending → donut slices. Beyond {@code maxSlices} the smallest categories are merged
 * into "Other" so the chart stays readable; amounts are summed in minor units to avoid
 * floating-point drift.
 */
export function toSpendingSlices(spending: CategorySpendingResponse, maxSlices = 6): SpendingSlice[] {
  const sorted = [...spending.categories].sort((a, b) => b.amount - a.amount);
  const head = sorted.slice(0, maxSlices);
  const tail = sorted.slice(maxSlices);
  const slices: SpendingSlice[] = head.map((c) => ({ name: c.categoryName, value: c.amount, color: c.color, percent: c.percentOfTotal }));
  if (tail.length > 0) {
    const otherCents = tail.reduce((total, c) => total + Math.round(c.amount * 100), 0);
    const otherPercent = Math.round(tail.reduce((total, c) => total + c.percentOfTotal, 0) * 100) / 100;
    slices.push({ name: 'Other', value: otherCents / 100, color: '#A9B6CC', percent: otherPercent });
  }
  return slices;
}
