import { dailyTotals, reviewFromReport, reviewFromTransactions } from './review';
import type { MonthlyReportResponse, TransactionResponse } from '../types/api';

const report = (income: number, expenses: number, previousExpenses: number): MonthlyReportResponse => ({
  year: 2026, month: 10, currency: 'CAD',
  totals: { income, expenses, net: income - expenses },
  previousMonth: { income: 0, expenses: previousExpenses, net: -previousExpenses },
  expensesByCategory: [
    { category: 'Dining', color: '#F59E0B', amount: 120, percentOfTotal: 30 },
    { category: 'Groceries', color: '#D97706', amount: 280, percentOfTotal: 70 },
  ],
  incomeByCategory: [], budget: null, goals: [], topExpenses: [],
});

const tx = (id: number, type: TransactionResponse['type'], amount: number, date: string, categoryName: string | null = 'Groceries', currency = 'CAD'): TransactionResponse => ({
  id, type, amount, currency, transactionDate: date, accountId: 1, accountName: 'Chequing', categoryId: id, categoryName,
  description: null, transferId: type.startsWith('TRANSFER') ? 9 : null, createdAt: '', updatedAt: '',
});

describe('period review', () => {
  it('derives savings rate, largest category, and change from a monthly report', () => {
    const r = reviewFromReport(report(4000, 400, 500));
    expect(r.savingsRate).toBe(90);
    expect(r.largest).toEqual({ name: 'Groceries', amount: 280, color: '#D97706' });
    expect(r.expenseChange).toBe(-20);
  });

  it('has no rate or change without a baseline', () => {
    const r = reviewFromReport(report(0, 400, 0));
    expect(r.savingsRate).toBeNull();
    expect(r.expenseChange).toBeNull();
  });

  it('totals a week exactly, ignoring transfers and other currencies', () => {
    const r = reviewFromTransactions(
      [tx(1, 'INCOME', 1000, '2026-10-19', 'Salary'), tx(2, 'EXPENSE', 0.1, '2026-10-19'), tx(3, 'EXPENSE', 0.2, '2026-10-20'), tx(4, 'TRANSFER_OUT', 500, '2026-10-20', null), tx(5, 'EXPENSE', 99, '2026-10-20', 'Dining', 'USD')],
      [tx(6, 'EXPENSE', 0.6, '2026-10-12')],
      'CAD',
    );
    expect(r.expenses).toBe(0.3);
    expect(r.net).toBe(999.7);
    expect(r.largest?.name).toBe('Groceries');
    expect(r.expenseChange).toBeCloseTo(-50);
  });

  it('builds one row per day', () => {
    const rows = dailyTotals([tx(1, 'EXPENSE', 5, '2026-10-19'), tx(2, 'EXPENSE', 2.5, '2026-10-19')], ['2026-10-18', '2026-10-19'], 'CAD', 'en-US');
    expect(rows.map((r) => [r.label, r.expenses])).toEqual([['Sun', 0], ['Mon', 7.5]]);
  });
});
