import { screen, within } from '@testing-library/react';
import { renderApp } from '../test/renderApp';
import { dashboardService } from '../services/dashboardService';
import type { DashboardSummaryResponse } from '../types/api';

vi.mock('../services/dashboardService', () => ({
  dashboardService: { summary: vi.fn(), spendingByCategory: vi.fn(), monthlyTrends: vi.fn() },
}));

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
  activeGoals: [{ id: 1, name: 'Trip', currency: 'CAD', savedAmount: 250, targetAmount: 1000, progressPercent: 25, targetDate: null, status: 'ACTIVE' }],
  achievedGoalCount: 0,
};

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.mocked(dashboardService.summary).mockResolvedValue(summary);
    vi.mocked(dashboardService.spendingByCategory).mockResolvedValue({
      year: 2026, month: 10, currency: 'CAD', total: 300,
      categories: [{ categoryId: 9, categoryName: 'Groceries', color: '#D97706', amount: 200, percentOfTotal: 66.67 }, { categoryId: 10, categoryName: 'Dining Out', color: '#F59E0B', amount: 100, percentOfTotal: 33.33 }],
    });
    vi.mocked(dashboardService.monthlyTrends).mockResolvedValue({ currency: 'CAD', months: [{ year: 2026, month: 10, income: 3000, expenses: 300, net: 2700 }] });
  });

  it('shows totals in the preferred currency and lists other currencies separately', async () => {
    renderApp('/', { signedIn: true });

    expect(await screen.findByText('Total balance')).toBeInTheDocument();
    expect(screen.getByText(/3,625\.00/)).toBeInTheDocument();
    expect(screen.getByText(/2,700\.00/)).toBeInTheDocument();
    expect(screen.getByText(/never added together/)).toBeInTheDocument();
  });

  it('shows budget status, category spending, and goal progress', async () => {
    renderApp('/', { signedIn: true });

    expect(await screen.findByText('1 over limit')).toBeInTheDocument();
    expect(screen.getByRole('progressbar', { name: 'Budget used' })).toHaveAttribute('aria-valuenow', '13');
    const legend = await screen.findByRole('list', { name: 'Spending by category' });
    expect(within(legend).getAllByRole('listitem').map((li) => li.textContent)).toEqual([
      expect.stringContaining('Groceries'),
      expect.stringContaining('Dining Out'),
    ]);
    expect(screen.getByRole('progressbar', { name: 'Trip progress' })).toHaveAttribute('aria-valuenow', '25');
  });

  it('asks for the selected month when navigating', async () => {
    renderApp('/', { signedIn: true });
    await screen.findByText('Total balance');
    const first = vi.mocked(dashboardService.summary).mock.calls[0]!;

    await screen.getByRole('button', { name: 'Previous month' }).click();

    const last = vi.mocked(dashboardService.summary).mock.calls.at(-1)!;
    const expected = first[1] === 1 ? [first[0] - 1, 12] : [first[0], first[1] - 1];
    expect(last).toEqual(expected);
  });
});
