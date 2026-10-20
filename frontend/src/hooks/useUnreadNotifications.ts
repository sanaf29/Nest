import { useCallback, useEffect, useState } from 'react';
import { useLocation } from 'react-router';
import { notificationService } from '../services/reminderService';

const POLL_MS = 60_000;

/**
 * Unread count for the bell: refreshed on navigation, every minute while the tab is visible,
 * and when the page asks (after marking items read).
 */
export function useUnreadNotifications() {
  const [count, setCount] = useState(0);
  const location = useLocation();

  const refresh = useCallback(() => {
    notificationService.unreadCount().then(setCount).catch(() => {
      // A failed poll is not worth interrupting the user for; the next one will retry.
    });
  }, []);

  useEffect(refresh, [refresh, location.pathname]);

  useEffect(() => {
    const timer = window.setInterval(() => {
      if (document.visibilityState === 'visible') refresh();
    }, POLL_MS);
    window.addEventListener('nest:notifications-changed', refresh);
    return () => {
      window.clearInterval(timer);
      window.removeEventListener('nest:notifications-changed', refresh);
    };
  }, [refresh]);

  return count;
}

export function announceNotificationsChanged() {
  window.dispatchEvent(new Event('nest:notifications-changed'));
}
