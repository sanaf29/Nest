import { apiClient } from './apiClient';
import type { NotificationResponse, PageResponse, ReminderRequest, ReminderResponse } from '../types/api';

export const reminderService = {
  list: () => apiClient.get<ReminderResponse[]>('/reminders').then((r) => r.data),
  create: (request: ReminderRequest) => apiClient.post<ReminderResponse>('/reminders', request).then((r) => r.data),
  update: (id: number, request: ReminderRequest) => apiClient.put<ReminderResponse>(`/reminders/${id}`, request).then((r) => r.data),
  dismiss: (id: number) => apiClient.post<ReminderResponse>(`/reminders/${id}/dismiss`).then((r) => r.data),
  remove: (id: number) => apiClient.delete<void>(`/reminders/${id}`).then(() => undefined),
};

export const notificationService = {
  list: (unreadOnly = false, page = 0, size = 20) =>
    apiClient.get<PageResponse<NotificationResponse>>('/notifications', { params: { unreadOnly, page, size } }).then((r) => r.data),
  unreadCount: () => apiClient.get<{ count: number }>('/notifications/unread-count').then((r) => r.data.count),
  markRead: (id: number) => apiClient.post<NotificationResponse>(`/notifications/${id}/read`).then((r) => r.data),
  markAllRead: () => apiClient.post<{ updated: number }>('/notifications/read-all').then((r) => r.data.updated),
};
