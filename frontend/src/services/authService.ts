import { apiClient } from './apiClient';
import type { AuthResponse, ChangePasswordRequest, LoginRequest, RegisterRequest, UpdateProfileRequest, UserResponse } from '../types/api';

export const authService = {
  register: (request: RegisterRequest) => apiClient.post<UserResponse>('/auth/register', request).then((r) => r.data),
  login: (request: LoginRequest) => apiClient.post<AuthResponse>('/auth/login', request).then((r) => r.data),
  me: () => apiClient.get<UserResponse>('/users/me').then((r) => r.data),
  updateProfile: (request: UpdateProfileRequest) => apiClient.put<UserResponse>('/users/me', request).then((r) => r.data),
  changePassword: (request: ChangePasswordRequest) => apiClient.put<void>('/users/me/password', request).then(() => undefined),
};
