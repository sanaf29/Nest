import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AxiosError, AxiosHeaders } from 'axios';
import { renderApp, testUser } from '../test/renderApp';
import { authService } from '../services/authService';

vi.mock('../services/authService', () => ({
  authService: { login: vi.fn(), register: vi.fn(), me: vi.fn(), updateProfile: vi.fn() },
}));
vi.mock('../services/dashboardService', () => ({
  dashboardService: {
    summary: vi.fn(() => new Promise(() => {})),
    spendingByCategory: vi.fn(() => new Promise(() => {})),
    monthlyTrends: vi.fn(() => new Promise(() => {})),
  },
}));

function problem(status: number, detail: string) {
  const config = { headers: new AxiosHeaders() };
  return new AxiosError('Request failed', 'ERR_BAD_REQUEST', config, null, {
    status,
    statusText: '',
    headers: {},
    config,
    data: { status, title: 'Authentication failed', detail },
  });
}

describe('LoginPage', () => {
  it('validates required fields before calling the API', async () => {
    renderApp('/login');
    await userEvent.click(await screen.findByRole('button', { name: 'Log in' }));

    expect(screen.getByText('Enter your email.')).toBeInTheDocument();
    expect(screen.getByText('Enter your password.')).toBeInTheDocument();
    expect(authService.login).not.toHaveBeenCalled();
    expect(screen.getByLabelText('Email')).toHaveAttribute('aria-invalid', 'true');
  });

  it('shows the server’s message when credentials are wrong', async () => {
    vi.mocked(authService.login).mockRejectedValueOnce(problem(401, 'Invalid email or password.'));
    renderApp('/login');

    await userEvent.type(await screen.findByLabelText('Email'), 'sanae@example.com');
    await userEvent.type(screen.getByLabelText('Password'), 'wrong-pass1');
    await userEvent.click(screen.getByRole('button', { name: 'Log in' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid email or password.');
  });

  it('signs in, stores the session, and opens the dashboard', async () => {
    vi.mocked(authService.login).mockResolvedValueOnce({
      accessToken: 'jwt',
      tokenType: 'Bearer',
      expiresAt: new Date(Date.now() + 3600_000).toISOString(),
      user: testUser,
    });
    const router = renderApp('/login');

    await userEvent.type(await screen.findByLabelText('Email'), ' sanae@example.com ');
    await userEvent.type(screen.getByLabelText('Password'), 'Str0ngPass');
    await userEvent.click(screen.getByRole('button', { name: 'Log in' }));

    await waitFor(() => expect(router.state.location.pathname).toBe('/'));
    expect(authService.login).toHaveBeenCalledWith({ email: 'sanae@example.com', password: 'Str0ngPass' });
    expect(JSON.parse(localStorage.getItem('nest.session')!).accessToken).toBe('jwt');
    expect(await screen.findByRole('heading', { name: /^Good (morning|afternoon|evening), Sanae$/ })).toBeInTheDocument();
  });
});
