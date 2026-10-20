import { apiClient } from './apiClient';
import type { InsightsResponse } from '../types/api';

export const insightService = {
  forMonth: (year: number, month: number, currency?: string) =>
    apiClient.get<InsightsResponse>('/insights', { params: { year, month, currency } }).then((r) => r.data),
};
