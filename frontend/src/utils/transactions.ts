import type { TransactionFilter, TransactionResponse, TransactionType } from '../types/api';
import { transactionService } from '../services/transactionService';
import { fromMinor, toMinor } from './money';

const PAGE_SIZE = 100;
const MAX_PAGES = 20;

/** Loads every page of a filtered search (bounded), e.g. all transactions in a month for the calendar. */
export async function fetchAllTransactions(filter: TransactionFilter): Promise<TransactionResponse[]> {
  const all: TransactionResponse[] = [];
  for (let page = 0; page < MAX_PAGES; page++) {
    const result = await transactionService.search({ ...filter, page, size: PAGE_SIZE, sort: 'transactionDate,asc' });
    all.push(...result.content);
    if (page + 1 >= result.totalPages) break;
  }
  return all;
}

export interface DayTotals {
  income: number;
  expenses: number;
  transfers: number;
  transactions: TransactionResponse[];
}

/**
 * Groups transactions by date with income and expense totals per day (single currency only:
 * the caller passes the currency to total). Transfers are counted separately, never as spending.
 */
export function groupByDate(transactions: TransactionResponse[], currency: string): Map<string, DayTotals> {
  const days = new Map<string, { income: number; expenses: number; transfers: number; transactions: TransactionResponse[] }>();
  for (const t of transactions) {
    const day = days.get(t.transactionDate) ?? { income: 0, expenses: 0, transfers: 0, transactions: [] };
    day.transactions.push(t);
    if (t.currency === currency) {
      const minor = toMinor(t.amount, currency);
      if (t.type === 'INCOME') day.income += minor;
      else if (t.type === 'EXPENSE') day.expenses += minor;
      else if (t.type === 'TRANSFER_OUT') day.transfers += minor;
    }
    days.set(t.transactionDate, day);
  }
  const result = new Map<string, DayTotals>();
  for (const [date, d] of days) {
    result.set(date, {
      income: fromMinor(d.income, currency),
      expenses: fromMinor(d.expenses, currency),
      transfers: fromMinor(d.transfers, currency),
      transactions: d.transactions,
    });
  }
  return result;
}

export const typeLabels: Record<TransactionType, string> = {
  INCOME: 'Income',
  EXPENSE: 'Expense',
  TRANSFER_IN: 'Transfer in',
  TRANSFER_OUT: 'Transfer out',
};

/** Amount with the sign the user expects: money in is positive, money out negative. */
export function signedAmount(t: TransactionResponse): number {
  return t.type === 'INCOME' || t.type === 'TRANSFER_IN' ? t.amount : -t.amount;
}
