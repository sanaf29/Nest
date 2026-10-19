import { isAxiosError } from 'axios';
import type { ProblemDetail } from '../types/api';

/** Field name → message, for showing server validation errors next to inputs. */
export type FieldErrors = Record<string, string>;

export interface ApiError {
  status: number;
  message: string;
  fieldErrors: FieldErrors;
}

/**
 * Converts any thrown value into something safe to show. Server problem details are used as-is
 * (they never contain internals). Anything else gets a generic message.
 */
export function toApiError(error: unknown): ApiError {
  if (isAxiosError<ProblemDetail>(error)) {
    const problem = error.response?.data;
    if (problem && typeof problem === 'object' && 'status' in problem) {
      const fieldErrors: FieldErrors = {};
      for (const e of problem.errors ?? []) {
        fieldErrors[e.field] ??= e.message;
      }
      return { status: problem.status, message: problem.detail ?? problem.title, fieldErrors };
    }
    if (!error.response) {
      return { status: 0, message: 'Cannot reach the server. Check your connection and try again.', fieldErrors: {} };
    }
    return { status: error.response.status, message: 'Something went wrong. Please try again.', fieldErrors: {} };
  }
  return { status: 0, message: 'Something went wrong. Please try again.', fieldErrors: {} };
}
