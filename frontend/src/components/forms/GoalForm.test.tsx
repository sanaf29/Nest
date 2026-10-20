import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ContributionForm } from './GoalForm';
import { goalService } from '../../services/goalService';
import type { SavingsGoalResponse } from '../../types/api';

vi.mock('../../services/goalService', () => ({ goalService: { contribute: vi.fn() } }));

const goal: SavingsGoalResponse = {
  id: 3, name: 'Trip', currency: 'CAD', targetAmount: 1000, savedAmount: 0, remainingAmount: 1000, progressPercent: 0,
  targetDate: null, status: 'ACTIVE', achievedAt: null, contributions: [], createdAt: '',
};

describe('ContributionForm', () => {
  it('reuses one idempotency key, so retries are not counted twice', async () => {
    vi.mocked(goalService.contribute).mockRejectedValueOnce(new Error('network')).mockResolvedValueOnce(goal);
    render(<ContributionForm goal={goal} onSaved={vi.fn()} onCancel={vi.fn()} />);

    await userEvent.type(screen.getByLabelText('Amount (CAD)'), '250');
    await userEvent.click(screen.getByRole('button', { name: 'Add contribution' }));
    await userEvent.click(await screen.findByRole('button', { name: 'Add contribution' }));

    const [first, second] = vi.mocked(goalService.contribute).mock.calls;
    expect(first![1].idempotencyKey).toBeTruthy();
    expect(second![1].idempotencyKey).toBe(first![1].idempotencyKey);
    expect(second![1].amount).toBe(250);
  });
});
