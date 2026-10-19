import { tokenStorage } from './tokenStorage';
import { testUser } from '../test/renderApp';

describe('tokenStorage', () => {
  it('returns a stored, unexpired session', () => {
    tokenStorage.write({ accessToken: 'abc', expiresAt: '2030-01-01T00:00:00Z', user: testUser });
    expect(tokenStorage.read(new Date('2029-12-31T23:59:00Z'))?.accessToken).toBe('abc');
  });

  it('discards an expired session', () => {
    tokenStorage.write({ accessToken: 'abc', expiresAt: '2026-10-19T10:00:00Z', user: testUser });
    expect(tokenStorage.read(new Date('2026-10-19T10:00:01Z'))).toBeNull();
    expect(localStorage.getItem('nest.session')).toBeNull();
  });

  it('survives corrupted storage', () => {
    localStorage.setItem('nest.session', '{not json');
    expect(tokenStorage.read()).toBeNull();
  });
});
