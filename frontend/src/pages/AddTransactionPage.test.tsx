import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderApp } from '../test/renderApp';
import { accountService } from '../services/accountService';
import { categoryService } from '../services/categoryService';
import { recurringService } from '../services/recurringService';
import { transactionService } from '../services/transactionService';
import type { AccountResponse, CategoryResponse } from '../types/api';
import { todayIso } from '../utils/dates';

vi.mock('../services/accountService', () => ({ accountService: { list: vi.fn() } }));
vi.mock('../services/categoryService', () => ({ categoryService: { list: vi.fn() } }));
vi.mock('../services/transactionService', () => ({ transactionService: { create: vi.fn(), search: vi.fn(() => new Promise(() => {})) }, transferService: {} }));
vi.mock('../services/dashboardService', () => ({ dashboardService: { summary: vi.fn(() => new Promise(() => {})) } }));
vi.mock('../services/insightService', () => ({ insightService: { forMonth: vi.fn(() => new Promise(() => {})) } }));
vi.mock('../services/recurringService', () => ({ recurringService: { create: vi.fn() } }));
vi.mock('../services/reminderService', () => ({ notificationService: { unreadCount: vi.fn().mockResolvedValue(0) } }));

const account: AccountResponse = { id: 1, name: 'Chequing', currency: 'CAD', accountType: 'CHECKING', openingBalance: 0, currentBalance: 0, active: true, createdAt: '', updatedAt: '' };
const category = (id: number, name: string, categoryType: 'INCOME' | 'EXPENSE'): CategoryResponse => ({ id, name, categoryType, color: '#D97706', isDefault: true, archived: false });

describe('AddTransactionPage', () => {
  beforeEach(() => {
    vi.mocked(accountService.list).mockResolvedValue([account]);
    vi.mocked(categoryService.list).mockResolvedValue([category(9, 'Groceries', 'EXPENSE'), category(10, 'Dining Out', 'EXPENSE'), category(1, 'Salary', 'INCOME')]);
  });

  it('records an expense from amount, category pill, and notes', async () => {
    vi.mocked(transactionService.create).mockResolvedValueOnce({} as never);
    const router = renderApp('/add', { signedIn: true });

    await userEvent.type(await screen.findByLabelText('Amount (CAD)'), '84.37');
    await userEvent.click(screen.getByRole('radio', { name: 'Groceries' }));
    await userEvent.type(screen.getByLabelText('Notes (optional)'), 'Weekly groceries');
    await userEvent.click(screen.getByRole('button', { name: 'Save transaction' }));

    expect(transactionService.create).toHaveBeenCalledWith({
      accountId: 1, categoryId: 9, type: 'EXPENSE', amount: 84.37, transactionDate: todayIso(), description: 'Weekly groceries',
    });
    await waitFor(() => expect(router.state.location.pathname).toBe('/')); // opened directly, so it returns home
  });

  it('shows only categories of the chosen type and validates before saving', async () => {
    renderApp('/add', { signedIn: true });

    await userEvent.click(await screen.findByRole('radio', { name: 'Income' }));
    expect(screen.queryByRole('radio', { name: 'Groceries' })).not.toBeInTheDocument();
    expect(screen.getByRole('radio', { name: 'Salary' })).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Save transaction' }));
    expect(screen.getByText(/Enter a positive amount/)).toBeInTheDocument();
    expect(screen.getByText('Choose a category.')).toBeInTheDocument();
    expect(transactionService.create).not.toHaveBeenCalled();
  });

  it('schedules a recurring rule when Repeat is on', async () => {
    vi.mocked(recurringService.create).mockResolvedValueOnce({} as never);
    renderApp('/add', { signedIn: true });

    await userEvent.type(await screen.findByLabelText('Amount (CAD)'), '17.99');
    await userEvent.click(screen.getByRole('radio', { name: 'Dining Out' }));
    await userEvent.click(screen.getByRole('switch', { name: /Repeat this transaction/ }));
    await userEvent.click(screen.getByRole('button', { name: 'Save transaction' }));
    expect(screen.getByText(/Name the repeating transaction/)).toBeInTheDocument();

    await userEvent.type(screen.getByLabelText('Name'), 'Pizza night');
    await userEvent.selectOptions(screen.getByLabelText('Repeats'), 'WEEKLY');
    await userEvent.click(screen.getByRole('button', { name: 'Save transaction' }));

    expect(recurringService.create).toHaveBeenCalledWith({
      accountId: 1, categoryId: 10, type: 'EXPENSE', amount: 17.99, description: 'Pizza night', frequency: 'WEEKLY', startDate: todayIso(),
    });
    expect(transactionService.create).not.toHaveBeenCalled();
  });
});
