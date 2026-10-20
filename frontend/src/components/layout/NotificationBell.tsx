import { Link } from 'react-router';
import { Icon } from '../ui/Icon';
import { useUnreadNotifications } from '../../hooks/useUnreadNotifications';

export function NotificationBell() {
  const unread = useUnreadNotifications();
  const label = unread === 0 ? 'Notifications' : `Notifications, ${unread} unread`;
  return (
    <Link to="/notifications" aria-label={label} title={label} className="relative rounded-xl p-2 text-ink-500 hover:bg-ink-100 hover:text-ink-900">
      <Icon name="bell" />
      {unread > 0 && (
        <span aria-hidden="true" className="absolute top-1 right-1 flex h-4 min-w-4 animate-pop items-center justify-center rounded-full bg-brand-600 px-1 text-[10px] font-bold text-on-brand ring-2 ring-canvas">
          {unread > 99 ? '99+' : unread}
        </span>
      )}
    </Link>
  );
}
