import { useState } from 'react';
import { ReminderForm, reminderTypeLabels } from '../components/forms/ReminderForm';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { ConfirmDialog } from '../components/ui/ConfirmDialog';
import { Icon } from '../components/ui/Icon';
import { Modal } from '../components/ui/Modal';
import { PageHeader } from '../components/ui/PageHeader';
import { EmptyState, ErrorState, LoadingState } from '../components/ui/States';
import { useAsync } from '../hooks/useAsync';
import { categoryService } from '../services/categoryService';
import { goalService } from '../services/goalService';
import { reminderService } from '../services/reminderService';
import type { ReminderResponse } from '../types/api';

function when(iso: string) {
  return new Date(iso).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' });
}

export function RemindersPage() {
  const reminders = useAsync(() => reminderService.list(), []);
  const goals = useAsync(() => goalService.list('ACTIVE'), []);
  const categories = useAsync(() => categoryService.list('EXPENSE'), []);
  const [editing, setEditing] = useState<ReminderResponse | 'new' | null>(null);
  const [deleting, setDeleting] = useState<ReminderResponse | null>(null);

  const scheduled = reminders.data?.filter((r) => r.status === 'SCHEDULED') ?? [];
  const past = reminders.data?.filter((r) => r.status !== 'SCHEDULED') ?? [];

  const row = (r: ReminderResponse) => (
    <li key={r.id} className="flex flex-col gap-2 px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-2">
          <span className="font-medium text-ink-900">{r.title}</span>
          <Badge tone={r.type === 'PARKING' ? 'warning' : r.type === 'BUDGET_THRESHOLD' ? 'brand' : 'neutral'}>{reminderTypeLabels[r.type]}</Badge>
          {r.status !== 'SCHEDULED' && <Badge>{r.status === 'SENT' ? 'sent' : 'dismissed'}</Badge>}
        </div>
        <p className="mt-1 text-xs text-ink-500">
          {r.type === 'BUDGET_THRESHOLD'
            ? `Alerts at ${r.thresholdPercent}% of ${r.categoryName ?? 'the whole budget'}, once a month`
            : `${r.type === 'PARKING' ? 'Expires' : 'Due'} ${when(r.dueAt!)}${r.offsetMinutes ? ` · notify ${when(r.remindAt!)}` : ''}`}
          {r.location && ` · ${r.location}`}
          {r.savingsGoalName && ` · ${r.savingsGoalName}`}
        </p>
      </div>
      <div className="flex gap-1">
        {r.status === 'SCHEDULED' && (
          <Button size="sm" variant="ghost" onClick={async () => { await reminderService.dismiss(r.id); reminders.reload(); }}>Turn off</Button>
        )}
        <Button size="sm" variant="ghost" onClick={() => setEditing(r)} aria-label={`Edit ${r.title}`}><Icon name="edit" className="h-4 w-4" /></Button>
        <Button size="sm" variant="ghost" onClick={() => setDeleting(r)} aria-label={`Delete ${r.title}`}><Icon name="trash" className="h-4 w-4" /></Button>
      </div>
    </li>
  );

  return (
    <>
      <PageHeader title="Reminders" description="Bills, renewals, parking, goal deadlines, and budget alerts, delivered to your notifications."
        actions={<Button icon={<Icon name="plus" className="h-4 w-4" />} onClick={() => setEditing('new')}>New reminder</Button>} />
      {reminders.loading && !reminders.data ? (
        <LoadingState />
      ) : reminders.error ? (
        <ErrorState error={reminders.error} onRetry={reminders.reload} />
      ) : reminders.data?.length === 0 ? (
        <Card>
          <EmptyState icon="bell" title="No reminders yet" action={<Button onClick={() => setEditing('new')}>Create one</Button>}>
            Parked downtown? Bill due Friday? Let Nest remember.
          </EmptyState>
        </Card>
      ) : (
        <div className="flex flex-col gap-6">
          <Card title="Upcoming" bodyClassName="p-0">
            {scheduled.length ? <ul className="divide-y divide-ink-100">{scheduled.map(row)}</ul> : <p className="p-5 text-sm text-ink-500">Nothing scheduled.</p>}
          </Card>
          {past.length > 0 && <Card title="Past" bodyClassName="p-0"><ul className="divide-y divide-ink-100">{past.map(row)}</ul></Card>}
        </div>
      )}
      <Modal open={editing !== null} title={editing === 'new' ? 'New reminder' : 'Edit reminder'} onClose={() => setEditing(null)}>
        {editing !== null && (
          <ReminderForm reminder={editing === 'new' ? undefined : editing} goals={goals.data ?? []} categories={categories.data ?? []}
            onSaved={() => { setEditing(null); reminders.reload(); }} onCancel={() => setEditing(null)} />
        )}
      </Modal>
      <ConfirmDialog open={deleting !== null} title="Delete reminder?" danger confirmLabel="Delete" message={`"${deleting?.title}" will be removed.`}
        onConfirm={async () => { await reminderService.remove(deleting!.id); reminders.reload(); }} onClose={() => setDeleting(null)} />
    </>
  );
}
