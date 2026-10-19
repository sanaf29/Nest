import { apiClient } from './apiClient';
import type { CategorySpendingResponse, DashboardSummaryResponse, MonthlyTrendsResponse } from '../types/api';

export const dashboardService = {
  summary: (year: number, month: number) =>
    apiClient.get<DashboardSummaryResponse>('/dashboard/summary', { params: { year, month } }).then((r) => r.data),
  spendingByCategory: (year: number, month: number, currency?: string) =>
    apiClient
      .get<CategorySpendingResponse>('/dashboard/spending-by-category', { params: { year, month, currency } })
      .then((r) => r.data),
  monthlyTrends: (year: number, month: number, months = 6, currency?: string) =>
    apiClient
      .get<MonthlyTrendsResponse>('/dashboard/monthly-trends', { params: { year, month, months, currency } })
      .then((r) => r.data),
};
