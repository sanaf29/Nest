import { apiClient } from './apiClient';
import type { SharedBalanceResponse, SharedExpenseRequest, SharedExpenseResponse } from '../types/api';

export const sharedExpenseService = {
  list: () => apiClient.get<SharedExpenseResponse[]>('/shared-expenses').then((r) => r.data),
  balances: () => apiClient.get<SharedBalanceResponse[]>('/shared-expenses/balances').then((r) => r.data),
  create: (request: SharedExpenseRequest) => apiClient.post<SharedExpenseResponse>('/shared-expenses', request).then((r) => r.data),
  remove: (id: number) => apiClient.delete<void>(`/shared-expenses/${id}`).then(() => undefined),
  settle: (id: number, participantId: number) =>
    apiClient.post<SharedExpenseResponse>(`/shared-expenses/${id}/participants/${participantId}/settle`).then((r) => r.data),
  unsettle: (id: number, participantId: number) =>
    apiClient.post<SharedExpenseResponse>(`/shared-expenses/${id}/participants/${participantId}/unsettle`).then((r) => r.data),
};
