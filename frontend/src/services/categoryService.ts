import { apiClient } from './apiClient';
import type { CategoryRequest, CategoryResponse, CategoryType } from '../types/api';

export const categoryService = {
  list: (type?: CategoryType, includeArchived = false) =>
    apiClient.get<CategoryResponse[]>('/categories', { params: { type, includeArchived } }).then((r) => r.data),
  create: (request: CategoryRequest) => apiClient.post<CategoryResponse>('/categories', request).then((r) => r.data),
  update: (id: number, request: CategoryRequest) =>
    apiClient.put<CategoryResponse>(`/categories/${id}`, request).then((r) => r.data),
  archive: (id: number) => apiClient.delete<void>(`/categories/${id}`).then(() => undefined),
};
