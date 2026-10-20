import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AxiosError, AxiosHeaders } from 'axios';
import { TransactionForm } from './TransactionForm';
import { transactionService } from '../../services/transactionService';
import type { AccountResponse, CategoryResponse } from '../../types/api';

vi.mock('../../services/transactionService', () => ({ transactionService: { create: vi.fn(), update: vi.fn() } }));

const account = (id: number, name: string, currency: string): AccountResponse => ({
  id, name, currency, accountType: 'CHECKING', openingBalance: 0, currentBalance: 0, active: true, createdAt: '', updatedAt: '',
});
const category = (id: number, name: string, categoryType: 'INCOME' | 'EXPENSE', archived = false): CategoryResponse => ({
  id, name, categoryType, color: '#000000', isDefault: true, archived,
});

const accounts = [account(1, 'Chequing', 'CAD'), account(2, 'Yen wallet', 'JPY')];
const categories = [category(9, 'Groceries', 'EXPENSE'), category(1, 'Salary', 'INCOME'), category(50, 'Old hobby', 'EXPENSE', true)];

function renderForm(onSaved = vi.fn()) {
  render(<TransactionForm accounts={accounts} categories={categories} onSaved={onSaved} onCancel={vi.fn()} />);
  return onSaved;
}

describe('TransactionForm', () => {
  it('offers only active categories of the chosen type', async () => {
    renderForm();
    const options = () => within(screen.getByLabelText('Category')).getAllByRole('option').map((o) => o.textContent);

    expect(options()).toEqual(['Choose…', 'Groceries']);
    await userEvent.click(screen.getByRole('radio', { name: 'Income' }));
    expect(options()).toEqual(['Choose…', 'Salary']);
  });

  it('checks amounts against the selected account’s currency', async () => {
    renderForm();
    await userEvent.selectOptions(screen.getByLabelText('Account'), '2');
    await userEvent.selectOptions(screen.getByLabelText('Category'), '9');
    await userEvent.type(screen.getByLabelText('Amount (JPY)'), '10.5');
    await userEvent.click(screen.getByRole('button', { name: 'Add expense' }));

    expect(screen.getByText('Enter a positive amount.')).toBeInTheDocument();
    expect(transactionService.create).not.toHaveBeenCalled();
  });

  it('sends a well-formed request', async () => {
    vi.mocked(transactionService.create).mockResolvedValueOnce({} as never);
    const onSaved = renderForm();

    await userEvent.type(screen.getByLabelText('Amount (CAD)'), '84.37');
    await userEvent.clear(screen.getByLabelText('Date'));
    await userEvent.type(screen.getByLabelText('Date'), '2026-10-18');
    await userEvent.selectOptions(screen.getByLabelText('Category'), '9');
    await userEvent.type(screen.getByLabelText('Description (optional)'), '  Weekly groceries ');
    await userEvent.click(screen.getByRole('button', { name: 'Add expense' }));

    expect(transactionService.create).toHaveBeenCalledWith({
      accountId: 1, categoryId: 9, type: 'EXPENSE', amount: 84.37, transactionDate: '2026-10-18', description: 'Weekly groceries',
    });
    expect(onSaved).toHaveBeenCalled();
  });

  it('shows business-rule errors from the server', async () => {
    const config = { headers: new AxiosHeaders() };
    vi.mocked(transactionService.create).mockRejectedValueOnce(
      new AxiosError('x', 'ERR', config, null, { status: 422, statusText: '', headers: {}, config, data: { status: 422, title: 'Business rule violated', detail: "Account 'Chequing' is closed." } }),
    );
    const onSaved = renderForm();

    await userEvent.type(screen.getByLabelText('Amount (CAD)'), '10');
    await userEvent.selectOptions(screen.getByLabelText('Category'), '9');
    await userEvent.click(screen.getByRole('button', { name: 'Add expense' }));

    expect(await screen.findByRole('alert')).toHaveTextContent("Account 'Chequing' is closed.");
    expect(onSaved).not.toHaveBeenCalled();
  });
});
