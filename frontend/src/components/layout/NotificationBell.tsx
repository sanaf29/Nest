import { Link } from 'react-router';
import { Icon } from '../ui/Icon';
import { useUnreadNotifications } from '../../hooks/useUnreadNotifications';

export function NotificationBell() {
  const unread = useUnreadNotifications();
  const label = unread === 0 ? 'Notifications' : `Notifications, ${unread} unread`;
  return (
    <Link to="/notifications" aria-label={label} title={label} className="relative rounded-lg p-2 text-ink-600 hover:bg-ink-100">
      <Icon name="bell" />
      {unread > 0 && (
        <span aria-hidden="true" className="absolute top-1 right-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-negative-600 px-1 text-[10px] font-semibold text-white">
          {unread > 99 ? '99+' : unread}
        </span>
      )}
    </Link>
  );
}
