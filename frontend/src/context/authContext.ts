import { createContext } from 'react';
import type { LoginRequest, RegisterRequest, UserResponse } from '../types/api';

export interface AuthState {
  user: UserResponse | null;
  isAuthenticated: boolean;
  login: (request: LoginRequest) => Promise<void>;
  register: (request: RegisterRequest) => Promise<void>;
  logout: () => void;
  updateUser: (user: UserResponse) => void;
}

export const AuthContext = createContext<AuthState | null>(null);
