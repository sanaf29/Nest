import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ReminderForm, toLocalInput } from './ReminderForm';
import { reminderService } from '../../services/reminderService';

vi.mock('../../services/reminderService', () => ({ reminderService: { create: vi.fn(), update: vi.fn() } }));

describe('ReminderForm', () => {
  it('asks for parking details and sends the expiry as an instant', async () => {
    vi.mocked(reminderService.create).mockResolvedValueOnce({} as never);
    render(<ReminderForm goals={[]} categories={[]} onSaved={vi.fn()} onCancel={vi.fn()} />);

    await userEvent.selectOptions(screen.getByLabelText('Kind'), 'PARKING');
    expect(screen.getByLabelText('Title')).toHaveValue('Parking');
    expect(screen.getByLabelText('Notify me')).toHaveValue('15');

    await userEvent.click(screen.getByRole('button', { name: 'Create reminder' }));
    expect(screen.getByText('Where did you park?')).toBeInTheDocument();
    expect(screen.getByText('When does your parking expire?')).toBeInTheDocument();

    const later = new Date(Date.now() + 2 * 3600_000);
    await userEvent.type(screen.getByLabelText('Where you parked'), 'Level 2, B14');
    await userEvent.type(screen.getByLabelText('Parking expires'), toLocalInput(later.toISOString()));
    await userEvent.click(screen.getByRole('button', { name: 'Create reminder' }));

    const request = vi.mocked(reminderService.create).mock.calls[0]![0];
    expect(request).toMatchObject({ type: 'PARKING', location: 'Level 2, B14', offsetMinutes: 15, thresholdPercent: null });
    expect(Math.abs(new Date(request.dueAt!).getTime() - later.getTime())).toBeLessThan(60_000);
  });

  it('budget alerts have a threshold instead of a date', async () => {
    render(<ReminderForm goals={[]} categories={[]} onSaved={vi.fn()} onCancel={vi.fn()} />);
    await userEvent.selectOptions(screen.getByLabelText('Kind'), 'BUDGET_THRESHOLD');

    expect(screen.queryByLabelText('Due')).not.toBeInTheDocument();
    expect(screen.getByLabelText('Alert at (% of budget)')).toHaveValue('80');
  });
});
