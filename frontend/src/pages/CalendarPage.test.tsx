import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderApp } from '../test/renderApp';
import { recurringService } from '../services/recurringService';
import { reminderService } from '../services/reminderService';
import { transactionService } from '../services/transactionService';
import type { RecurringResponse } from '../types/api';
import { addMonths, currentYearMonth, toIsoDate } from '../utils/dates';

vi.mock('../services/transactionService', () => ({ transactionService: { search: vi.fn() }, transferService: {} }));
vi.mock('../services/recurringService', () => ({ recurringService: { list: vi.fn() } }));
vi.mock('../services/reminderService', () => ({ reminderService: { list: vi.fn() }, notificationService: { unreadCount: vi.fn().mockResolvedValue(0) } }));

describe('CalendarPage', () => {
  beforeEach(() => {
    vi.mocked(recurringService.list).mockResolvedValue([]);
    vi.mocked(reminderService.list).mockResolvedValue([]);
  });

  it('shows daily totals and lists a selected day’s transactions', async () => {
    const { year, month } = currentYearMonth();
    const day = toIsoDate(year, month, 12);
    vi.mocked(transactionService.search).mockResolvedValue({
      content: [
        { id: 1, type: 'EXPENSE', amount: 42.5, currency: 'CAD', transactionDate: day, accountId: 1, accountName: 'Chequing', categoryId: 9, categoryName: 'Groceries', description: 'Market', transferId: null, createdAt: '', updatedAt: '' },
      ],
      page: 0, size: 100, totalElements: 1, totalPages: 1,
    });
    renderApp('/calendar', { signedIn: true });

    const cell = await screen.findByRole('button', { name: /income .*0\.00, expenses .*42\.50/ });
    await userEvent.click(cell);

    expect(cell).toHaveAttribute('aria-pressed', 'true');
    const panel = (await screen.findByRole('heading', { name: /12/ })).closest('section')!;
    expect(within(panel).getByText('Market')).toBeInTheDocument();
    expect(within(panel).getByText(/Groceries/)).toBeInTheDocument();
    expect(transactionService.search).toHaveBeenCalledWith(expect.objectContaining({ from: toIsoDate(year, month, 1), size: 100 }));
  });

  it('marks upcoming paydays and bills and lists them for the day', async () => {
    const next = addMonths(currentYearMonth(), 1); // always in the future
    const payday = toIsoDate(next.year, next.month, 15);
    vi.mocked(transactionService.search).mockResolvedValue({ content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 });
    vi.mocked(recurringService.list).mockResolvedValue([
      { id: 3, type: 'INCOME', description: 'Paycheque', amount: 2400, currency: 'CAD', accountName: 'Chequing', categoryName: 'Salary', status: 'ACTIVE', upcoming: [payday] } as RecurringResponse,
      { id: 4, type: 'EXPENSE', description: 'Old gym', amount: 40, currency: 'CAD', accountName: 'Chequing', categoryName: 'Health', status: 'PAUSED', upcoming: [payday] } as RecurringResponse,
    ]);
    renderApp('/calendar', { signedIn: true });

    await userEvent.click(await screen.findByRole('button', { name: 'Next month' }));
    const cell = await screen.findByRole('button', { name: /15.*no transactions, 1 scheduled/ });
    await userEvent.click(cell);

    expect(await screen.findByText('Paycheque')).toBeInTheDocument();
    expect(screen.getByText(/Payday · Chequing/)).toBeInTheDocument();
    expect(screen.queryByText('Old gym')).not.toBeInTheDocument(); // paused rules don't show
  });
});
