import { apiClient } from './apiClient';
import type {
  PageResponse,
  TransactionFilter,
  TransactionRequest,
  TransactionResponse,
  TransferRequest,
  TransferResponse,
} from '../types/api';

/** Drops empty filter values so the query string only carries what the user set. */
function cleanParams(filter: TransactionFilter): Record<string, string | number> {
  const params: Record<string, string | number> = {};
  for (const [key, value] of Object.entries(filter)) {
    if (value !== undefined && value !== null && value !== '') {
      params[key] = value as string | number;
    }
  }
  return params;
}

export const transactionService = {
  search: (filter: TransactionFilter = {}) =>
    apiClient.get<PageResponse<TransactionResponse>>('/transactions', { params: cleanParams(filter) }).then((r) => r.data),
  create: (request: TransactionRequest) =>
    apiClient.post<TransactionResponse>('/transactions', request).then((r) => r.data),
  update: (id: number, request: TransactionRequest) =>
    apiClient.put<TransactionResponse>(`/transactions/${id}`, request).then((r) => r.data),
  remove: (id: number) => apiClient.delete<void>(`/transactions/${id}`).then(() => undefined),
};

export const transferService = {
  create: (request: TransferRequest) => apiClient.post<TransferResponse>('/transfers', request).then((r) => r.data),
  get: (id: number) => apiClient.get<TransferResponse>(`/transfers/${id}`).then((r) => r.data),
  remove: (id: number) => apiClient.delete<void>(`/transfers/${id}`).then(() => undefined),
};
