// Calendar helpers. Dates are plain yyyy-MM-dd strings, the same as the API's LocalDate, so
// no timezone conversion ever shifts a transaction to another day.

export interface YearMonth {
  year: number;
  month: number; // 1–12
}

export function todayIso(now: Date = new Date()): string {
  return toIsoDate(now.getFullYear(), now.getMonth() + 1, now.getDate());
}

export function currentYearMonth(now: Date = new Date()): YearMonth {
  return { year: now.getFullYear(), month: now.getMonth() + 1 };
}

export function toIsoDate(year: number, month: number, day: number): string {
  return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
}

export function daysInMonth({ year, month }: YearMonth): number {
  return new Date(year, month, 0).getDate();
}

export function addMonths({ year, month }: YearMonth, delta: number): YearMonth {
  const index = year * 12 + (month - 1) + delta;
  return { year: Math.floor(index / 12), month: (index % 12) + 1 };
}

export function monthRange(ym: YearMonth): { from: string; to: string } {
  return { from: toIsoDate(ym.year, ym.month, 1), to: toIsoDate(ym.year, ym.month, daysInMonth(ym)) };
}

export function monthLabel({ year, month }: YearMonth, style: 'long' | 'short' = 'long'): string {
  return new Date(year, month - 1, 1).toLocaleDateString(undefined, { month: style, year: 'numeric' });
}

export function formatDate(iso: string): string {
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(y!, m! - 1, d!).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' });
}

/**
 * The weeks of a month as rows of 7 cells (Sunday first). Cells outside the month are null.
 * Used by the calendar page.
 */
export function calendarWeeks(ym: YearMonth): (string | null)[][] {
  const firstWeekday = new Date(ym.year, ym.month - 1, 1).getDay();
  const cells: (string | null)[] = Array.from({ length: firstWeekday }, () => null);
  for (let day = 1; day <= daysInMonth(ym); day++) {
    cells.push(toIsoDate(ym.year, ym.month, day));
  }
  while (cells.length % 7 !== 0) {
    cells.push(null);
  }
  const weeks: (string | null)[][] = [];
  for (let i = 0; i < cells.length; i += 7) {
    weeks.push(cells.slice(i, i + 7));
  }
  return weeks;
}
