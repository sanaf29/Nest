import { screen } from '@testing-library/react';
import { renderApp } from '../../test/renderApp';

vi.mock('../../services/dashboardService', () => ({
  dashboardService: {
    summary: vi.fn(() => new Promise(() => {})),
    spendingByCategory: vi.fn(() => new Promise(() => {})),
    monthlyTrends: vi.fn(() => new Promise(() => {})),
  },
}));

describe('route protection', () => {
  it('sends signed-out visitors to the login page and remembers where they were going', async () => {
    const router = renderApp('/budget?month=10');

    expect(await screen.findByRole('heading', { name: 'Welcome back' })).toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/login');
    expect(router.state.location.state).toEqual({ from: '/budget?month=10' });
  });

  it('lets signed-in users into the app', async () => {
    renderApp('/', { signedIn: true });
    expect(await screen.findByRole('heading', { name: 'Hi Sanae' })).toBeInTheDocument();
    expect(screen.getByRole('navigation', { name: 'Main' })).toBeInTheDocument();
  });

  it('keeps signed-in users away from the login page', async () => {
    const router = renderApp('/login', { signedIn: true });
    await screen.findByRole('heading', { name: 'Hi Sanae' });
    expect(router.state.location.pathname).toBe('/');
  });
});
