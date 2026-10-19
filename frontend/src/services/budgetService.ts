import { isAxiosError } from 'axios';
import { apiClient } from './apiClient';
import type { BudgetResponse, BudgetSummaryResponse, CreateBudgetRequest, UpdateBudgetRequest } from '../types/api';

export const budgetService = {
  list: () => apiClient.get<BudgetResponse[]>('/budgets').then((r) => r.data),
  /** The budget for a month, or null if none exists (404). */
  forMonth: async (year: number, month: number, currency?: string): Promise<BudgetResponse | null> => {
    try {
      return (await apiClient.get<BudgetResponse>(`/budgets/${year}/${month}`, { params: { currency } })).data;
    } catch (error) {
      if (isAxiosError(error) && error.response?.status === 404) return null;
      throw error;
    }
  },
  create: (request: CreateBudgetRequest) => apiClient.post<BudgetResponse>('/budgets', request).then((r) => r.data),
  update: (id: number, request: UpdateBudgetRequest) =>
    apiClient.put<BudgetResponse>(`/budgets/${id}`, request).then((r) => r.data),
  remove: (id: number) => apiClient.delete<void>(`/budgets/${id}`).then(() => undefined),
  summary: (id: number) => apiClient.get<BudgetSummaryResponse>(`/budgets/${id}/summary`).then((r) => r.data),
};
