import { render } from '@testing-library/react';
import { createMemoryRouter, RouterProvider } from 'react-router';
import { routes } from '../App';
import { AuthProvider } from '../context/AuthProvider';
import { tokenStorage } from '../services/tokenStorage';
import type { UserResponse } from '../types/api';

export const testUser: UserResponse = {
  id: 1,
  firstName: 'Sanae',
  lastName: 'Faghfouri',
  email: 'sanae@example.com',
  preferredCurrency: 'CAD',
  createdAt: '2026-10-01T00:00:00Z',
  updatedAt: '2026-10-01T00:00:00Z',
};

/** Renders the real route tree at a URL, optionally with a signed-in user. */
export function renderApp(path: string, { signedIn = false } = {}) {
  if (signedIn) {
    tokenStorage.write({ accessToken: 'test-token', expiresAt: new Date(Date.now() + 3600_000).toISOString(), user: testUser });
  }
  const router = createMemoryRouter(routes, { initialEntries: [path] });
  render(
    <AuthProvider>
      <RouterProvider router={router} />
    </AuthProvider>,
  );
  return router;
}
