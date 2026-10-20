import { useState } from 'react';
import { useNavigate } from 'react-router';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { Icon, type IconName } from '../components/ui/Icon';
import { PageHeader } from '../components/ui/PageHeader';
import { EmptyState, ErrorState, LoadingState } from '../components/ui/States';
import { useAsync } from '../hooks/useAsync';
import { announceNotificationsChanged } from '../hooks/useUnreadNotifications';
import { notificationService } from '../services/reminderService';
import type { NotificationCategory, NotificationResponse } from '../types/api';
import { cn } from '../utils/cn';

const icons: Record<NotificationCategory, IconName> = { REMINDER: 'bell', BUDGET: 'pie', RECURRING: 'repeat', GOAL: 'target', SYSTEM: 'alert' };

export function NotificationsPage() {
  const [unreadOnly, setUnreadOnly] = useState(false);
  const [page, setPage] = useState(0);
  const list = useAsync(() => notificationService.list(unreadOnly, page, 20), [unreadOnly, page]);
  const navigate = useNavigate();

  async function open(n: NotificationResponse) {
    if (!n.read) {
      await notificationService.markRead(n.id);
      announceNotificationsChanged();
    }
    if (n.link) navigate(n.link);
    else list.reload();
  }

  return (
    <>
      <PageHeader
        title="Notifications"
        actions={
          <>
            <label className="flex items-center gap-2 text-sm text-ink-600">
              <input type="checkbox" checked={unreadOnly} onChange={(e) => { setPage(0); setUnreadOnly(e.target.checked); }} className="rounded border-ink-300" />
              Unread only
            </label>
            <Button variant="secondary" onClick={async () => { await notificationService.markAllRead(); announceNotificationsChanged(); list.reload(); }}>
              Mark all read
            </Button>
          </>
        }
      />
      <Card bodyClassName="p-0">
        {list.loading && !list.data ? (
          <LoadingState />
        ) : list.error ? (
          <div className="p-5"><ErrorState error={list.error} onRetry={list.reload} /></div>
        ) : list.data && list.data.content.length > 0 ? (
          <>
            <ul className="divide-y divide-ink-100">
              {list.data.content.map((n) => (
                <li key={n.id}>
                  <button type="button" onClick={() => open(n)} className={cn('flex w-full gap-3 px-5 py-4 text-left hover:bg-ink-50', !n.read && 'bg-brand-50/50')}>
                    <span className={cn('mt-0.5 rounded-full p-2', n.read ? 'bg-ink-100 text-ink-500' : 'bg-brand-100 text-brand-800')}>
                      <Icon name={icons[n.category]} className="h-4 w-4" />
                    </span>
                    <span className="min-w-0 flex-1">
                      <span className={cn('block text-sm', n.read ? 'text-ink-700' : 'font-semibold text-ink-900')}>
                        {n.title}
                        {!n.read && <span className="sr-only"> (unread)</span>}
                      </span>
                      <span className="mt-0.5 block text-sm text-ink-600">{n.message}</span>
                      <span className="mt-1 block text-xs text-ink-400">{new Date(n.createdAt).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })}</span>
                    </span>
                  </button>
                </li>
              ))}
            </ul>
            {list.data.totalPages > 1 && (
              <nav aria-label="Pagination" className="flex items-center justify-between border-t border-ink-100 px-5 py-3 text-sm">
                <Button variant="secondary" size="sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>Previous</Button>
                <span className="text-ink-500">Page {page + 1} of {list.data.totalPages}</span>
                <Button variant="secondary" size="sm" disabled={page + 1 >= list.data.totalPages} onClick={() => setPage((p) => p + 1)}>Next</Button>
              </nav>
            )}
          </>
        ) : (
          <EmptyState icon="bell" title={unreadOnly ? 'All caught up' : 'No notifications yet'}>
            Reminders, budget alerts, and recurring bills will show up here.
          </EmptyState>
        )}
      </Card>
    </>
  );
}
