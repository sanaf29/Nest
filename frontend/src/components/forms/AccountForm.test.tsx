import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AccountForm } from './AccountForm';
import { accountService } from '../../services/accountService';

vi.mock('../../services/accountService', () => ({ accountService: { create: vi.fn(), update: vi.fn() } }));

describe('AccountForm', () => {
  it('refuses a negative opening balance for savings but allows it for a credit card', async () => {
    vi.mocked(accountService.create).mockResolvedValueOnce({} as never);
    render(<AccountForm defaultCurrency="CAD" onSaved={vi.fn()} onCancel={vi.fn()} />);

    await userEvent.type(screen.getByLabelText('Name'), 'Rainy day');
    await userEvent.selectOptions(screen.getByLabelText('Type'), 'SAVINGS');
    await userEvent.clear(screen.getByLabelText('Opening balance'));
    await userEvent.type(screen.getByLabelText('Opening balance'), '-50');
    await userEvent.click(screen.getByRole('button', { name: 'Add account' }));
    expect(screen.getByText('A savings account cannot start below zero.')).toBeInTheDocument();
    expect(accountService.create).not.toHaveBeenCalled();

    await userEvent.selectOptions(screen.getByLabelText('Type'), 'CREDIT_CARD');
    await userEvent.click(screen.getByRole('button', { name: 'Add account' }));
    expect(accountService.create).toHaveBeenCalledWith({ name: 'Rainy day', accountType: 'CREDIT_CARD', currency: 'CAD', openingBalance: -50 });
  });

  it('locks type and currency when editing', () => {
    render(
      <AccountForm
        account={{ id: 1, name: 'Visa', accountType: 'CREDIT_CARD', currency: 'USD', openingBalance: -10, currentBalance: -10, active: true, createdAt: '', updatedAt: '' }}
        defaultCurrency="CAD"
        onSaved={vi.fn()}
        onCancel={vi.fn()}
      />,
    );
    expect(screen.getByLabelText('Type')).toBeDisabled();
    expect(screen.getByLabelText('Currency')).toBeDisabled();
    expect(screen.getByLabelText('Currency')).toHaveValue('USD');
  });
});
