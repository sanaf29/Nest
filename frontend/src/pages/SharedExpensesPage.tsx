import { useState } from 'react';
import { SharedExpenseForm } from '../components/forms/SharedExpenseForm';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { ConfirmDialog } from '../components/ui/ConfirmDialog';
import { Icon } from '../components/ui/Icon';
import { Modal } from '../components/ui/Modal';
import { MoneyText } from '../components/ui/MoneyText';
import { PageHeader } from '../components/ui/PageHeader';
import { EmptyState, ErrorState, LoadingState } from '../components/ui/States';
import { useAsync } from '../hooks/useAsync';
import { useAuth } from '../hooks/useAuth';
import { sharedExpenseService } from '../services/sharedExpenseService';
import type { SharedExpenseResponse } from '../types/api';
import { formatDate } from '../utils/dates';

export function SharedExpensesPage() {
  const { user } = useAuth();
  const expenses = useAsync(() => sharedExpenseService.list(), []);
  const balances = useAsync(() => sharedExpenseService.balances(), []);
  const [creating, setCreating] = useState(false);
  const [deleting, setDeleting] = useState<SharedExpenseResponse | null>(null);
  const reload = () => { expenses.reload(); balances.reload(); };

  async function toggle(expense: SharedExpenseResponse, participantId: number, settled: boolean) {
    if (settled) await sharedExpenseService.unsettle(expense.id, participantId);
    else await sharedExpenseService.settle(expense.id, participantId);
    reload();
  }

  return (
    <>
      <PageHeader title="Split expenses" description="Share costs with friends and keep track of who owes whom."
        actions={<Button icon={<Icon name="plus" className="h-4 w-4" />} onClick={() => setCreating(true)}>Split an expense</Button>} />

      <Card title="Balances" className="mb-6">
        {balances.data && balances.data.length > 0 ? (
          <ul className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {balances.data.map((b) => (
              <li key={`${b.person}-${b.currency}`} className="rounded-xl bg-ink-50 p-4">
                <p className="text-sm text-ink-600">{b.amount > 0 ? `${b.person} owes you` : `You owe ${b.person}`}</p>
                <p className="text-xl font-semibold">
                  <MoneyText amount={Math.abs(b.amount)} currency={b.currency} className={b.amount > 0 ? 'text-positive-600' : 'text-negative-600'} />
                </p>
              </li>
            ))}
          </ul>
        ) : (
          <p className="text-sm text-ink-500">{balances.loading ? 'Loading…' : 'All square: nobody owes anything.'}</p>
        )}
      </Card>

      {expenses.loading && !expenses.data ? (
        <LoadingState />
      ) : expenses.error ? (
        <ErrorState error={expenses.error} onRetry={expenses.reload} />
      ) : expenses.data && expenses.data.length > 0 ? (
        <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
          {expenses.data.map((e) => (
            <article key={e.id} className="rounded-2xl bg-white p-5 shadow-sm ring-1 ring-ink-200/70">
              <header className="mb-3 flex items-start justify-between gap-2">
                <div>
                  <h2 className="font-semibold text-ink-900">{e.description}</h2>
                  <p className="text-xs text-ink-500">{formatDate(e.expenseDate)} · <MoneyText amount={e.totalAmount} currency={e.currency} /> · {e.splitMethod.toLowerCase()} split</p>
                </div>
                <div className="flex items-center gap-1">
                  {e.settled ? <Badge tone="positive">Settled</Badge> : <Badge tone="warning">Open</Badge>}
                  <button type="button" onClick={() => setDeleting(e)} className="rounded-lg p-1.5 text-ink-500 hover:bg-ink-100" aria-label={`Delete ${e.description}`}>
                    <Icon name="trash" className="h-4 w-4" />
                  </button>
                </div>
              </header>
              <ul className="divide-y divide-ink-100 text-sm">
                {e.participants.map((p) => (
                  <li key={p.id} className="flex items-center justify-between gap-2 py-2">
                    <span className="min-w-0 truncate">
                      {p.me ? 'You' : p.name}
                      {p.paid && <Badge tone="brand" className="ml-2">paid</Badge>}
                    </span>
                    <span className="flex items-center gap-3">
                      <MoneyText amount={p.share} currency={e.currency} className="tabular-nums" />
                      {p.owes && (
                        <Button size="sm" variant={p.settled ? 'ghost' : 'secondary'} onClick={() => toggle(e, p.id, p.settled)}
                          aria-label={p.settled ? `Mark ${p.name} as not settled` : `Mark ${p.name} as settled`}>
                          {p.settled ? <><Icon name="check" className="h-4 w-4 text-positive-600" /> Settled</> : 'Mark settled'}
                        </Button>
                      )}
                    </span>
                  </li>
                ))}
              </ul>
            </article>
          ))}
        </div>
      ) : (
        <Card>
          <EmptyState icon="users" title="No shared expenses" action={<Button onClick={() => setCreating(true)}>Split your first expense</Button>}>
            Dinner, trips, rent with roommates: split it equally, by amount, or by percentage.
          </EmptyState>
        </Card>
      )}

      <Modal open={creating} title="Split an expense" onClose={() => setCreating(false)}>
        {creating && <SharedExpenseForm currency={user?.preferredCurrency ?? 'CAD'} myName={user?.firstName ?? 'Me'} onSaved={() => { setCreating(false); reload(); }} onCancel={() => setCreating(false)} />}
      </Modal>
      <ConfirmDialog open={deleting !== null} title="Delete this split?" danger confirmLabel="Delete" message={`"${deleting?.description}" and its balances will be removed.`}
        onConfirm={async () => { await sharedExpenseService.remove(deleting!.id); reload(); }} onClose={() => setDeleting(null)} />
    </>
  );
}
