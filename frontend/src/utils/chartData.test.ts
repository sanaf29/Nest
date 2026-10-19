import { toSpendingSlices, toTrendRows } from './chartData';
import type { CategorySpendingResponse } from '../types/api';

describe('chart data mapping', () => {
  it('maps monthly trends to labelled rows in order', () => {
    const rows = toTrendRows(
      { currency: 'CAD', months: [{ year: 2026, month: 9, income: 100, expenses: 30, net: 70 }, { year: 2026, month: 10, income: 0, expenses: 10, net: -10 }] },
      'en-US',
    );
    expect(rows).toEqual([
      { label: 'Sep 26', income: 100, expenses: 30, net: 70 },
      { label: 'Oct 26', income: 0, expenses: 10, net: -10 },
    ]);
  });

  it('keeps the largest categories and merges the rest into Other', () => {
    const spending: CategorySpendingResponse = {
      year: 2026,
      month: 10,
      currency: 'CAD',
      total: 100,
      categories: [
        { categoryId: 1, categoryName: 'Rent', color: '#111', amount: 60, percentOfTotal: 60 },
        { categoryId: 2, categoryName: 'Food', color: '#222', amount: 25, percentOfTotal: 25 },
        { categoryId: 3, categoryName: 'Bus', color: '#333', amount: 10.1, percentOfTotal: 10.1 },
        { categoryId: 4, categoryName: 'Gym', color: '#444', amount: 4.9, percentOfTotal: 4.9 },
      ],
    };

    const slices = toSpendingSlices(spending, 2);

    expect(slices.map((s) => s.name)).toEqual(['Rent', 'Food', 'Other']);
    expect(slices[2]).toMatchObject({ value: 15, percent: 15 });
  });
});
