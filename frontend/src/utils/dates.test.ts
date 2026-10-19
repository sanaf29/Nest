import { addMonths, calendarWeeks, daysInMonth, monthRange } from './dates';

describe('date utilities', () => {
  it('handles month lengths including leap years', () => {
    expect(daysInMonth({ year: 2028, month: 2 })).toBe(29);
    expect(daysInMonth({ year: 2026, month: 2 })).toBe(28);
    expect(daysInMonth({ year: 2026, month: 10 })).toBe(31);
  });

  it('moves across year boundaries', () => {
    expect(addMonths({ year: 2026, month: 12 }, 1)).toEqual({ year: 2027, month: 1 });
    expect(addMonths({ year: 2026, month: 1 }, -1)).toEqual({ year: 2025, month: 12 });
    expect(addMonths({ year: 2026, month: 10 }, -22)).toEqual({ year: 2024, month: 12 });
  });

  it('gives the inclusive first and last day of a month', () => {
    expect(monthRange({ year: 2028, month: 2 })).toEqual({ from: '2028-02-01', to: '2028-02-29' });
  });

  it('lays out a month as Sunday-first weeks', () => {
    // October 2026 starts on a Thursday.
    const weeks = calendarWeeks({ year: 2026, month: 10 });
    expect(weeks[0]).toEqual([null, null, null, null, '2026-10-01', '2026-10-02', '2026-10-03']);
    expect(weeks.flat().filter(Boolean)).toHaveLength(31);
    expect(weeks.every((w) => w.length === 7)).toBe(true);
    expect(weeks.at(-1)).toContain('2026-10-31');
  });
});
