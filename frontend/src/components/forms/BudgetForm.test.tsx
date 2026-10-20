import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BudgetForm, budgetPreview } from './BudgetForm';
import { budgetService } from '../../services/budgetService';
import type { CategoryResponse } from '../../types/api';

vi.mock('../../services/budgetService', () => ({ budgetService: { create: vi.fn(), update: vi.fn() } }));

const categories: CategoryResponse[] = [
  { id: 9, name: 'Groceries', categoryType: 'EXPENSE', color: '#D97706', isDefault: true, archived: false },
  { id: 10, name: 'Dining Out', categoryType: 'EXPENSE', color: '#F59E0B', isDefault: true, archived: false },
];

describe('budgetPreview', () => {
  it('matches the specification example', () => {
    expect(budgetPreview(3000, 'PERCENTAGE', 20, [], 'CAD')).toMatchObject({ savingsTarget: 600, spendable: 2400, unallocated: 2400 });
  });

  it('rounds half-even like the server', () => {
    expect(budgetPreview(100.05, 'PERCENTAGE', 50, [], 'CAD').savingsTarget).toBe(50.02);
    expect(budgetPreview(100.15, 'PERCENTAGE', 50, [], 'CAD').savingsTarget).toBe(50.08);
    expect(budgetPreview(12345, 'PERCENTAGE', 10, [], 'JPY').savingsTarget).toBe(1234);
  });

  it('flags over-allocation and savings above income', () => {
    expect(budgetPreview(1000, 'PERCENTAGE', 20, [500, 300.01], 'CAD').overAllocated).toBe(true);
    expect(budgetPreview(1000, 'PERCENTAGE', 20, [500, 300], 'CAD').overAllocated).toBe(false);
    expect(budgetPreview(1000, 'FIXED_AMOUNT', 1200, [], 'CAD').savingsExceedIncome).toBe(true);
  });
});

describe('BudgetForm', () => {
  it('previews the plan as the user types and blocks over-allocation', async () => {
    render(<BudgetForm year={2026} month={10} currency="CAD" categories={categories} onSaved={vi.fn()} onCancel={vi.fn()} />);
    const preview = screen.getByLabelText('Plan preview');

    await userEvent.type(screen.getByLabelText('Planned income (CAD)'), '3000');
    expect(within(preview).getByText(/600\.00/)).toBeInTheDocument();
    expect(within(preview).getAllByText(/2,400\.00/).length).toBeGreaterThan(0);

    await userEvent.click(screen.getByRole('button', { name: 'Add category limit' }));
    await userEvent.selectOptions(screen.getByLabelText('Category 1'), '9');
    await userEvent.type(screen.getByLabelText('Limit'), '2500');
    await userEvent.click(screen.getByRole('button', { name: 'Create budget' }));

    expect(screen.getByText('Category limits exceed what is left after savings.')).toBeInTheDocument();
    expect(budgetService.create).not.toHaveBeenCalled();
  });

  it('creates the budget with its allocations', async () => {
    vi.mocked(budgetService.create).mockResolvedValueOnce({} as never);
    const onSaved = vi.fn();
    render(<BudgetForm year={2026} month={10} currency="CAD" categories={categories} onSaved={onSaved} onCancel={vi.fn()} />);

    await userEvent.type(screen.getByLabelText('Planned income (CAD)'), '3000');
    await userEvent.click(screen.getByRole('button', { name: 'Add category limit' }));
    await userEvent.selectOptions(screen.getByLabelText('Category 1'), '10');
    await userEvent.type(screen.getByLabelText('Limit'), '200');
    await userEvent.click(screen.getByRole('button', { name: 'Create budget' }));

    expect(budgetService.create).toHaveBeenCalledWith({
      year: 2026, month: 10, currency: 'CAD', plannedIncome: 3000, savingsStrategy: 'PERCENTAGE', savingsValue: 20,
      allocations: [{ categoryId: 10, limit: 200 }],
    });
    expect(onSaved).toHaveBeenCalled();
  });
});
