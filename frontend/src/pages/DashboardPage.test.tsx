import { screen, within } from '@testing-library/react';
import { renderApp } from '../test/renderApp';
import { budgetService } from '../services/budgetService';
import { dashboardService } from '../services/dashboardService';
import { transactionService } from '../services/transactionService';
import type { BudgetSummaryResponse, DashboardSummaryResponse } from '../types/api';
import { greeting } from './DashboardPage';

vi.mock('../services/dashboardService', () => ({
  dashboardService: { summary: vi.fn(), spendingByCategory: vi.fn(), monthlyTrends: vi.fn() },
}));
vi.mock('../services/budgetService', () => ({ budgetService: { summary: vi.fn() } }));
vi.mock('../services/transactionService', () => ({ transactionService: { search: vi.fn() }, transferService: {} }));
vi.mock('../services/insightService', () => ({ insightService: { forMonth: vi.fn().mockResolvedValue({ insights: [] }) } }));
vi.mock('../services/categoryService', () => ({ categoryService: { list: vi.fn().mockResolvedValue([]) } }));

const summary: DashboardSummaryResponse = {
  year: 2026,
  month: 10,
  preferredCurrency: 'CAD',
  balances: [
    { currency: 'CAD', total: 3625, accountCount: 2 },
    { currency: 'USD', total: 30, accountCount: 1 },
  ],
  cashFlow: [{ currency: 'CAD', income: 3000, expenses: 300, net: 2700 }],
  budget: { budgetId: 1, currency: 'CAD', spendable: 2400, spent: 300, remaining: 2100, percentUsed: 12.5, categoriesNearLimit: 0, categoriesExceeded: 1 },
  activeGoals: [
    { id: 1, name: 'Emergency Fund', currency: 'CAD', savedAmount: 250, targetAmount: 1000, progressPercent: 25, targetDate: null, status: 'ACTIVE' },
    { id: 2, name: 'New Laptop', currency: 'CAD', savedAmount: 900, targetAmount: 1800, progressPercent: 50, targetDate: null, status: 'ACTIVE' },
  ],
  achievedGoalCount: 0,
};

const line = (categoryId: number, categoryName: string, limit: number, spent: number): BudgetSummaryResponse['categories'][number] => ({
  categoryId, categoryName, color: '#D97706', limit, spent, remaining: limit - spent, percentUsed: (spent / limit) * 100, status: spent > limit ? 'EXCEEDED' : 'ON_TRACK',
});

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.mocked(dashboardService.summary).mockResolvedValue(summary);
    vi.mocked(budgetService.summary).mockResolvedValue({
      categories: [line(9, 'Groceries', 500, 200), line(10, 'Dining Out', 50, 75), line(11, 'Shopping', 200, 10), line(12, 'Transportation', 150, 15), line(13, 'Health', 100, 0)],
    } as BudgetSummaryResponse);
    vi.mocked(transactionService.search).mockResolvedValue({
      content: [{ id: 1, type: 'EXPENSE', amount: 84.37, currency: 'CAD', transactionDate: '2026-10-18', accountId: 1, accountName: 'Chequing', categoryId: 9, categoryName: 'Groceries', description: 'Loblaws', transferId: null, createdAt: '', updatedAt: '' }],
      page: 0, size: 5, totalElements: 1, totalPages: 1,
    });
  });

  it('greets by time of day', () => {
    expect(greeting(new Date(2026, 9, 20, 8))).toBe('Good morning');
    expect(greeting(new Date(2026, 9, 20, 14))).toBe('Good afternoon');
    expect(greeting(new Date(2026, 9, 20, 21))).toBe('Good evening');
  });

  it('shows the monthly overview in the preferred currency and lists other currencies separately', async () => {
    renderApp('/', { signedIn: true });

    expect(await screen.findByRole('group', { name: 'Total balance' })).toHaveTextContent('3,625.00');
    expect(screen.getByRole('group', { name: 'Income' })).toHaveTextContent('3,000.00');
    expect(screen.getByRole('group', { name: 'Expenses' })).toHaveTextContent('300.00');
    expect(screen.getByRole('group', { name: 'Budget left' })).toHaveTextContent('2,100.00');
    expect(screen.getByRole('group', { name: 'Net cash flow' })).toHaveTextContent('2,700.00');
    expect(screen.getByText(/never added together/)).toBeInTheDocument();
  });

  it('shows budget status, the four busiest categories, goals, and recent transactions', async () => {
    renderApp('/', { signedIn: true });

    expect(await screen.findByText('1 over limit')).toBeInTheDocument();
    expect(screen.getByRole('progressbar', { name: 'Budget used' })).toHaveAttribute('aria-valuenow', '13');
    const limits = await screen.findByRole('list', { name: 'Category limits' });
    expect(within(limits).getAllByRole('listitem').map((li) => li.textContent)).toEqual([
      expect.stringContaining('Dining Out'),
      expect.stringContaining('Groceries'),
      expect.stringContaining('Transportation'),
      expect.stringContaining('Shopping'),
    ]);
    expect(screen.getByRole('progressbar', { name: 'Emergency Fund progress' })).toHaveAttribute('aria-valuenow', '25');
    expect(screen.getByRole('progressbar', { name: 'New Laptop progress' })).toHaveAttribute('aria-valuenow', '50');
    expect(await screen.findByText('Loblaws')).toBeInTheDocument();
  });

  it('asks for the selected month when navigating', async () => {
    renderApp('/', { signedIn: true });
    await screen.findByRole('group', { name: 'Total balance' });
    const first = vi.mocked(dashboardService.summary).mock.calls[0]!;

    await screen.getByRole('button', { name: 'Previous month' }).click();

    const last = vi.mocked(dashboardService.summary).mock.calls.at(-1)!;
    const expected = first[1] === 1 ? [first[0] - 1, 12] : [first[0], first[1] - 1];
    expect(last).toEqual(expected);
  });
});
