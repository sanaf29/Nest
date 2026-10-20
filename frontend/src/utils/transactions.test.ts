import { groupByDate, signedAmount } from './transactions';
import type { TransactionResponse } from '../types/api';

const tx = (id: number, type: TransactionResponse['type'], amount: number, date: string, currency = 'CAD'): TransactionResponse => ({
  id, type, amount, currency, transactionDate: date, accountId: 1, accountName: 'A', categoryId: null, categoryName: null,
  description: null, transferId: type.startsWith('TRANSFER') ? 7 : null, createdAt: '', updatedAt: '',
});

describe('groupByDate', () => {
  it('totals income and expenses per day, keeping transfers and other currencies out', () => {
    const days = groupByDate(
      [
        tx(1, 'EXPENSE', 0.1, '2026-10-05'),
        tx(2, 'EXPENSE', 0.2, '2026-10-05'),
        tx(3, 'INCOME', 3000, '2026-10-05'),
        tx(4, 'TRANSFER_OUT', 500, '2026-10-05'),
        tx(5, 'TRANSFER_IN', 500, '2026-10-05'),
        tx(6, 'EXPENSE', 99, '2026-10-05', 'USD'),
        tx(7, 'EXPENSE', 10, '2026-10-06'),
      ],
      'CAD',
    );

    const day = days.get('2026-10-05')!;
    expect(day.expenses).toBe(0.3);
    expect(day.income).toBe(3000);
    expect(day.transfers).toBe(500);
    expect(day.transactions).toHaveLength(6);
    expect(days.get('2026-10-06')!.expenses).toBe(10);
  });

  it('gives money in a positive sign and money out a negative sign', () => {
    expect(signedAmount(tx(1, 'EXPENSE', 5, '2026-10-01'))).toBe(-5);
    expect(signedAmount(tx(1, 'TRANSFER_IN', 5, '2026-10-01'))).toBe(5);
  });
});
