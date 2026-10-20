import { apiClient } from './apiClient';
import type { RecurringRequest, RecurringResponse, UpdateRecurringRequest } from '../types/api';

export const recurringService = {
  list: () => apiClient.get<RecurringResponse[]>('/recurring-transactions').then((r) => r.data),
  create: (request: RecurringRequest) => apiClient.post<RecurringResponse>('/recurring-transactions', request).then((r) => r.data),
  update: (id: number, request: UpdateRecurringRequest) =>
    apiClient.put<RecurringResponse>(`/recurring-transactions/${id}`, request).then((r) => r.data),
  pause: (id: number) => apiClient.post<RecurringResponse>(`/recurring-transactions/${id}/pause`).then((r) => r.data),
  resume: (id: number) => apiClient.post<RecurringResponse>(`/recurring-transactions/${id}/resume`).then((r) => r.data),
  end: (id: number) => apiClient.delete<void>(`/recurring-transactions/${id}`).then(() => undefined),
  runNow: () => apiClient.post<{ generated: number; paused: number }>('/recurring-transactions/run').then((r) => r.data),
};
