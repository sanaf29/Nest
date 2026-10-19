import { apiClient } from './apiClient';
import type {
  ContributionRequest,
  CreateGoalRequest,
  GoalStatus,
  SavingsGoalResponse,
  UpdateGoalRequest,
} from '../types/api';

export const goalService = {
  list: (status?: GoalStatus) =>
    apiClient.get<SavingsGoalResponse[]>('/savings-goals', { params: { status } }).then((r) => r.data),
  create: (request: CreateGoalRequest) => apiClient.post<SavingsGoalResponse>('/savings-goals', request).then((r) => r.data),
  update: (id: number, request: UpdateGoalRequest) =>
    apiClient.put<SavingsGoalResponse>(`/savings-goals/${id}`, request).then((r) => r.data),
  remove: (id: number) => apiClient.delete<void>(`/savings-goals/${id}`).then(() => undefined),
  /** Sends an idempotency key so a double-click or retry cannot record the contribution twice. */
  contribute: (id: number, request: ContributionRequest) =>
    apiClient
      .post<SavingsGoalResponse>(`/savings-goals/${id}/contributions`, {
        ...request,
        idempotencyKey: request.idempotencyKey ?? crypto.randomUUID(),
      })
      .then((r) => r.data),
  removeContribution: (id: number, contributionId: number) =>
    apiClient.delete<SavingsGoalResponse>(`/savings-goals/${id}/contributions/${contributionId}`).then((r) => r.data),
};
