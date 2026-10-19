import type { UserResponse } from '../types/api';

const KEY = 'nest.session';

export interface StoredSession {
  accessToken: string;
  expiresAt: string;
  user: UserResponse;
}

/**
 * Persists the session in localStorage so a page refresh keeps the user signed in.
 * Trade-off (see docs/sprint2/sprint2.md): script on the page could read it, so the app never
 * renders untrusted HTML and tokens expire after 60 minutes. Expired sessions are discarded on read.
 */
export const tokenStorage = {
  read(now: Date = new Date()): StoredSession | null {
    try {
      const raw = localStorage.getItem(KEY);
      if (!raw) return null;
      const session = JSON.parse(raw) as StoredSession;
      if (!session.accessToken || new Date(session.expiresAt) <= now) {
        localStorage.removeItem(KEY);
        return null;
      }
      return session;
    } catch {
      return null;
    }
  },
  write(session: StoredSession): void {
    try {
      localStorage.setItem(KEY, JSON.stringify(session));
    } catch {
      // Storage can be unavailable (private mode); the session then lasts until reload.
    }
  },
  clear(): void {
    try {
      localStorage.removeItem(KEY);
    } catch {
      // ignore
    }
  },
};
