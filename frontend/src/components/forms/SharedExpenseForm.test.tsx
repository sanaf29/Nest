import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { SharedExpenseForm } from './SharedExpenseForm';
import { sharedExpenseService } from '../../services/sharedExpenseService';

vi.mock('../../services/sharedExpenseService', () => ({ sharedExpenseService: { create: vi.fn() } }));

describe('SharedExpenseForm', () => {
  it('previews an exact equal split and sends who paid and who I am', async () => {
    vi.mocked(sharedExpenseService.create).mockResolvedValueOnce({} as never);
    const onSaved = vi.fn();
    render(<SharedExpenseForm currency="CAD" myName="Sanae" onSaved={onSaved} onCancel={vi.fn()} />);

    await userEvent.type(screen.getByLabelText('What for'), 'Pizza');
    await userEvent.type(screen.getByLabelText('Total (CAD)'), '10');
    await userEvent.type(screen.getByLabelText('Person 2'), 'Alex');
    await userEvent.click(screen.getByRole('button', { name: 'Add person' }));
    await userEvent.type(screen.getByLabelText('Person 3'), 'Sam');

    expect(screen.getByLabelText('Sanae share')).toHaveTextContent('3.34');
    expect(screen.getByLabelText('Alex share')).toHaveTextContent('3.33');
    expect(screen.getByLabelText('Sam share')).toHaveTextContent('3.33');

    await userEvent.click(screen.getAllByRole('radio', { name: 'Paid' })[1]!);
    await userEvent.click(screen.getByRole('button', { name: 'Save split' }));

    const request = vi.mocked(sharedExpenseService.create).mock.calls[0]![0];
    expect(request.participants).toEqual([
      { name: 'Sanae', email: undefined, me: true, paid: false, value: undefined },
      { name: 'Alex', email: undefined, me: false, paid: true, value: undefined },
      { name: 'Sam', email: undefined, me: false, paid: false, value: undefined },
    ]);
    expect(onSaved).toHaveBeenCalled();
  });

  it('blocks custom amounts that do not add up', async () => {
    render(<SharedExpenseForm currency="CAD" myName="Sanae" onSaved={vi.fn()} onCancel={vi.fn()} />);
    await userEvent.type(screen.getByLabelText('What for'), 'Groceries');
    await userEvent.type(screen.getByLabelText('Total (CAD)'), '100');
    await userEvent.selectOptions(screen.getByLabelText('Split'), 'CUSTOM');
    await userEvent.type(screen.getByLabelText('Person 2'), 'Alex');
    const amounts = screen.getAllByLabelText('Amount');
    await userEvent.type(amounts[0]!, '60');
    await userEvent.type(amounts[1]!, '30');

    expect(screen.getByRole('alert')).toHaveTextContent('10.00 still to assign');
    await userEvent.click(screen.getByRole('button', { name: 'Save split' }));
    expect(sharedExpenseService.create).not.toHaveBeenCalled();
  });
});
