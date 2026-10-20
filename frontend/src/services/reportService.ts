import { apiClient } from './apiClient';
import type { MonthlyReportResponse, TransactionFilter } from '../types/api';

/** Saves a server-generated file. The request carries the bearer token, so it can't be a plain link. */
async function download(url: string, params: Record<string, unknown>, fallbackName: string): Promise<void> {
  const response = await apiClient.get<Blob>(url, { params, responseType: 'blob' });
  const disposition = String(response.headers['content-disposition'] ?? '');
  const name = /filename="([^"]+)"/.exec(disposition)?.[1] ?? fallbackName;
  const href = URL.createObjectURL(response.data);
  const link = document.createElement('a');
  link.href = href;
  link.download = name;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(href);
}

export const reportService = {
  monthly: (year: number, month: number, currency?: string) =>
    apiClient.get<MonthlyReportResponse>('/reports/monthly', { params: { year, month, currency } }).then((r) => r.data),
  downloadMonthlyCsv: (year: number, month: number, currency?: string) =>
    download('/reports/monthly.csv', { year, month, currency }, `nest-report-${year}-${month}.csv`),
  downloadTransactionsCsv: (filter: TransactionFilter) =>
    download('/reports/transactions.csv', { ...filter }, 'nest-transactions.csv'),
};
