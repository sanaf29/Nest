import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderApp } from '../test/renderApp';
import { transactionService } from '../services/transactionService';
import { currentYearMonth, toIsoDate } from '../utils/dates';

vi.mock('../services/transactionService', () => ({ transactionService: { search: vi.fn() }, transferService: {} }));

describe('CalendarPage', () => {
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
});
