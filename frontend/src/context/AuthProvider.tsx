import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import { AuthContext, type AuthState } from './authContext';
import { authService } from '../services/authService';
import { onUnauthorized } from '../services/apiClient';
import { tokenStorage } from '../services/tokenStorage';
import type { LoginRequest, RegisterRequest, UserResponse } from '../types/api';

/**
 * Owns the signed-in user. The session is restored from storage on load, cleared on logout,
 * on token expiry, and whenever the API answers 401.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(() => tokenStorage.read()?.user ?? null);

  const logout = useCallback(() => {
    tokenStorage.clear();
    setUser(null);
  }, []);

  useEffect(() => onUnauthorized(logout), [logout]);

  // Log out exactly when the token expires, even if the tab stays idle.
  useEffect(() => {
    const session = tokenStorage.read();
    if (!session) return;
    const ms = new Date(session.expiresAt).getTime() - Date.now();
    const timer = window.setTimeout(logout, Math.max(ms, 0));
    return () => window.clearTimeout(timer);
  }, [user, logout]);

  const login = useCallback(async (request: LoginRequest) => {
    const response = await authService.login(request);
    tokenStorage.write({ accessToken: response.accessToken, expiresAt: response.expiresAt, user: response.user });
    setUser(response.user);
  }, []);

  const register = useCallback(
    async (request: RegisterRequest) => {
      await authService.register(request);
      await login({ email: request.email, password: request.password });
    },
    [login],
  );

  const updateUser = useCallback((updated: UserResponse) => {
    const session = tokenStorage.read();
    if (session) tokenStorage.write({ ...session, user: updated });
    setUser(updated);
  }, []);

  const value = useMemo<AuthState>(
    () => ({ user, isAuthenticated: user !== null, login, register, logout, updateUser }),
    [user, login, register, logout, updateUser],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
