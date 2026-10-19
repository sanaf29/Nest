import { apiClient } from './apiClient';
import type { AccountResponse, CreateAccountRequest, UpdateAccountRequest } from '../types/api';

export const accountService = {
  list: (includeInactive = false) =>
    apiClient.get<AccountResponse[]>('/accounts', { params: { includeInactive } }).then((r) => r.data),
  get: (id: number) => apiClient.get<AccountResponse>(`/accounts/${id}`).then((r) => r.data),
  create: (request: CreateAccountRequest) => apiClient.post<AccountResponse>('/accounts', request).then((r) => r.data),
  update: (id: number, request: UpdateAccountRequest) =>
    apiClient.put<AccountResponse>(`/accounts/${id}`, request).then((r) => r.data),
  close: (id: number) => apiClient.delete<void>(`/accounts/${id}`).then(() => undefined),
};
