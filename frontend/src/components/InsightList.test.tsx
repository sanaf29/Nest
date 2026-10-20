import { render, screen } from '@testing-library/react';
import { InsightList } from './InsightList';

describe('InsightList', () => {
  it('shows the explanation of each insight and respects the limit', () => {
    render(
      <InsightList
        limit={1}
        insights={[
          { code: 'BUDGET_EXCEEDED', severity: 'ALERT', title: 'Dining Out is over budget', message: 'Dining Out: 250.00 CAD of 200.00 CAD (125%).' },
          { code: 'SAVINGS_TARGET_MET', severity: 'POSITIVE', title: 'Target met', message: '...' },
        ]}
      />,
    );
    expect(screen.getByRole('heading', { name: 'Dining Out is over budget' })).toBeInTheDocument();
    expect(screen.getByText('Dining Out: 250.00 CAD of 200.00 CAD (125%).')).toBeInTheDocument();
    expect(screen.queryByText('Target met')).not.toBeInTheDocument();
  });
});
