import { useState } from 'react';
import { RecurringForm, frequencyLabels } from '../components/forms/RecurringForm';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { ConfirmDialog } from '../components/ui/ConfirmDialog';
import { Icon } from '../components/ui/Icon';
import { Modal } from '../components/ui/Modal';
import { MoneyText } from '../components/ui/MoneyText';
import { PageHeader } from '../components/ui/PageHeader';
import { EmptyState, ErrorState, FormError, LoadingState } from '../components/ui/States';
import { useAsync } from '../hooks/useAsync';
import { useReferenceData } from '../hooks/useReferenceData';
import { recurringService } from '../services/recurringService';
import type { RecurringResponse, RecurringStatus } from '../types/api';
import { formatDate } from '../utils/dates';
import { toApiError } from '../utils/errors';

const statusTone: Record<RecurringStatus, 'positive' | 'warning' | 'neutral'> = { ACTIVE: 'positive', PAUSED: 'warning', ENDED: 'neutral' };

export function RecurringPage() {
  const reference = useReferenceData();
  const rules = useAsync(() => recurringService.list(), []);
  const [editing, setEditing] = useState<RecurringResponse | 'new' | null>(null);
  const [ending, setEnding] = useState<RecurringResponse | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function act(action: () => Promise<unknown>) {
    setError(null);
    try {
      await action();
      rules.reload();
    } catch (e) {
      setError(toApiError(e).message);
    }
  }

  return (
    <>
      <PageHeader
        title="Recurring"
        description="Rent, salary, subscriptions: scheduled once, recorded automatically on each date, exactly once."
        actions={
          <>
            <Button variant="secondary" onClick={() => act(async () => {
              const r = await recurringService.runNow();
              setMessage(r.generated === 0 ? 'Everything is up to date.' : `Recorded ${r.generated} transaction${r.generated === 1 ? '' : 's'}.`);
            })}>
              Record due now
            </Button>
            <Button icon={<Icon name="plus" className="h-4 w-4" />} onClick={() => setEditing('new')} disabled={reference.accounts.length === 0}>
              Schedule
            </Button>
          </>
        }
      />
      {message && <p role="status" className="mb-4 text-sm text-positive-600">{message}</p>}
      <div className="mb-4"><FormError message={error} /></div>

      <Card bodyClassName="p-0">
        {rules.loading && !rules.data ? (
          <LoadingState />
        ) : rules.error ? (
          <div className="p-5"><ErrorState error={rules.error} onRetry={rules.reload} /></div>
        ) : rules.data && rules.data.length > 0 ? (
          <ul className="divide-y divide-ink-100">
            {rules.data.map((r) => (
              <li key={r.id} className="flex flex-col gap-3 px-5 py-4 lg:flex-row lg:items-center lg:justify-between">
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="font-medium text-ink-900">{r.description}</span>
                    <Badge tone={statusTone[r.status]}>{r.status.toLowerCase()}</Badge>
                    <Badge>{frequencyLabels[r.frequency]}</Badge>
                  </div>
                  <p className="mt-1 text-xs text-ink-500">
                    {r.categoryName} · {r.accountName}
                    {r.nextOccurrence && r.status === 'ACTIVE' && ` · next ${formatDate(r.nextOccurrence)}`}
                    {r.remindDaysBefore != null && ` · reminder ${r.remindDaysBefore === 0 ? 'on the day' : `${r.remindDaysBefore}d before`}`}
                  </p>
                  {r.pauseReason && <p className="mt-1 text-xs text-warning-600">{r.pauseReason}</p>}
                </div>
                <div className="flex items-center justify-between gap-3 lg:justify-end">
                  <MoneyText amount={r.type === 'INCOME' ? r.amount : -r.amount} currency={r.currency} signed className="font-semibold" />
                  {r.status !== 'ENDED' && (
                    <div className="flex gap-1">
                      {r.status === 'ACTIVE' ? (
                        <Button size="sm" variant="ghost" onClick={() => act(() => recurringService.pause(r.id))}>Pause</Button>
                      ) : (
                        <Button size="sm" variant="ghost" onClick={() => act(() => recurringService.resume(r.id))}>Resume</Button>
                      )}
                      <Button size="sm" variant="ghost" onClick={() => setEditing(r)} aria-label={`Edit ${r.description}`}><Icon name="edit" className="h-4 w-4" /></Button>
                      <Button size="sm" variant="ghost" onClick={() => setEnding(r)} aria-label={`End ${r.description}`}><Icon name="trash" className="h-4 w-4" /></Button>
                    </div>
                  )}
                </div>
              </li>
            ))}
          </ul>
        ) : (
          <EmptyState icon="repeat" title="Nothing scheduled" action={<Button onClick={() => setEditing('new')} disabled={reference.accounts.length === 0}>Schedule a bill or income</Button>}>
            Set up rent, pay, or subscriptions once and Nest records them for you.
          </EmptyState>
        )}
      </Card>

      <Modal open={editing !== null} title={editing === 'new' ? 'Schedule a recurring transaction' : 'Edit recurring transaction'} onClose={() => setEditing(null)}>
        {editing !== null && (
          <RecurringForm accounts={reference.accounts} categories={reference.categories} rule={editing === 'new' ? undefined : editing}
            onSaved={() => { setEditing(null); rules.reload(); }} onCancel={() => setEditing(null)} />
        )}
      </Modal>
      <ConfirmDialog open={ending !== null} title="End this schedule?" danger confirmLabel="End"
        message={`"${ending?.description}" won't be recorded again. Past transactions stay.`}
        onConfirm={async () => { await recurringService.end(ending!.id); rules.reload(); }} onClose={() => setEnding(null)} />
    </>
  );
}
