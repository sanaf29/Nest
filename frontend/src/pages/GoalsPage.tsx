import { useState } from 'react';
import { ContributionForm, GoalForm } from '../components/forms/GoalForm';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { ConfirmDialog } from '../components/ui/ConfirmDialog';
import { Icon } from '../components/ui/Icon';
import { Modal } from '../components/ui/Modal';
import { MoneyText } from '../components/ui/MoneyText';
import { PageHeader } from '../components/ui/PageHeader';
import { ProgressBar } from '../components/ui/ProgressBar';
import { EmptyState, ErrorState, LoadingState } from '../components/ui/States';
import { useAsync } from '../hooks/useAsync';
import { useAuth } from '../hooks/useAuth';
import { goalService } from '../services/goalService';
import type { SavingsGoalResponse } from '../types/api';
import { formatDate } from '../utils/dates';

function GoalCard({ goal, onContribute, onEdit, onDelete }: { goal: SavingsGoalResponse; onContribute: () => void; onEdit: () => void; onDelete: () => void }) {
  const [showHistory, setShowHistory] = useState(false);
  const achieved = goal.status === 'ACHIEVED';
  return (
    <article className="flex flex-col gap-4 rounded-2xl bg-white p-5 shadow-sm ring-1 ring-ink-200/70">
      <header className="flex items-start justify-between gap-2">
        <div className="min-w-0">
          <h3 className="truncate font-semibold text-ink-900">{goal.name}</h3>
          <p className="text-xs text-ink-500">{goal.targetDate ? `Target date ${formatDate(goal.targetDate)}` : 'No deadline'}</p>
        </div>
        {achieved ? <Badge tone="positive">Achieved</Badge> : <Badge tone="brand">{goal.progressPercent.toFixed(0)}%</Badge>}
      </header>
      <div>
        <ProgressBar value={goal.progressPercent} label={`${goal.name} progress`} tone={achieved ? 'positive' : 'brand'} />
        <p className="mt-2 flex justify-between text-sm">
          <span>
            <MoneyText amount={goal.savedAmount} currency={goal.currency} className="font-semibold text-ink-900" /> <span className="text-ink-500">saved</span>
          </span>
          <span className="text-ink-500">
            of <MoneyText amount={goal.targetAmount} currency={goal.currency} />
          </span>
        </p>
        {!achieved && (
          <p className="text-xs text-ink-500">
            <MoneyText amount={goal.remainingAmount} currency={goal.currency} /> to go
          </p>
        )}
      </div>
      <div className="flex flex-wrap gap-2">
        <Button size="sm" icon={<Icon name="plus" className="h-4 w-4" />} onClick={onContribute}>
          Contribute
        </Button>
        <Button size="sm" variant="ghost" onClick={onEdit} aria-label={`Edit ${goal.name}`}>
          <Icon name="edit" className="h-4 w-4" />
        </Button>
        <Button size="sm" variant="ghost" onClick={onDelete} aria-label={`Delete ${goal.name}`}>
          <Icon name="trash" className="h-4 w-4" />
        </Button>
        {goal.contributions.length > 0 && (
          <Button size="sm" variant="ghost" onClick={() => setShowHistory((s) => !s)} aria-expanded={showHistory}>
            {showHistory ? 'Hide' : 'Show'} {goal.contributions.length} contribution{goal.contributions.length === 1 ? '' : 's'}
          </Button>
        )}
      </div>
      {showHistory && (
        <ul className="divide-y divide-ink-100 rounded-lg bg-ink-50 text-sm">
          {goal.contributions.map((c) => (
            <li key={c.id} className="flex justify-between gap-2 px-3 py-2">
              <span className="text-ink-600">
                {formatDate(c.contributionDate)}
                {c.note && ` · ${c.note}`}
              </span>
              <MoneyText amount={c.amount} currency={goal.currency} className="font-medium" />
            </li>
          ))}
        </ul>
      )}
    </article>
  );
}

export function GoalsPage() {
  const { user } = useAuth();
  const goals = useAsync(() => goalService.list(), []);
  const [editing, setEditing] = useState<SavingsGoalResponse | 'new' | null>(null);
  const [contributing, setContributing] = useState<SavingsGoalResponse | null>(null);
  const [deleting, setDeleting] = useState<SavingsGoalResponse | null>(null);

  const active = goals.data?.filter((g) => g.status === 'ACTIVE') ?? [];
  const achieved = goals.data?.filter((g) => g.status === 'ACHIEVED') ?? [];
  const cards = (list: SavingsGoalResponse[]) => (
    <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
      {list.map((g) => (
        <GoalCard key={g.id} goal={g} onContribute={() => setContributing(g)} onEdit={() => setEditing(g)} onDelete={() => setDeleting(g)} />
      ))}
    </div>
  );
  const done = () => {
    setEditing(null);
    setContributing(null);
    goals.reload();
  };

  return (
    <>
      <PageHeader
        title="Savings goals"
        description="Track what you're saving for. Contributions record money set aside; they don't move money between accounts."
        actions={
          <Button icon={<Icon name="plus" className="h-4 w-4" />} onClick={() => setEditing('new')}>
            New goal
          </Button>
        }
      />
      {goals.loading && !goals.data ? (
        <LoadingState />
      ) : goals.error ? (
        <ErrorState error={goals.error} onRetry={goals.reload} />
      ) : goals.data?.length === 0 ? (
        <Card>
          <EmptyState icon="target" title="No goals yet" action={<Button onClick={() => setEditing('new')}>Create your first goal</Button>}>
            An emergency fund, a trip, a new laptop: give your savings a purpose.
          </EmptyState>
        </Card>
      ) : (
        <div className="flex flex-col gap-8">
          {active.length > 0 && cards(active)}
          {achieved.length > 0 && (
            <section>
              <h2 className="mb-3 text-sm font-semibold tracking-wide text-ink-500 uppercase">Achieved</h2>
              {cards(achieved)}
            </section>
          )}
        </div>
      )}

      <Modal open={editing !== null} title={editing === 'new' ? 'New savings goal' : 'Edit goal'} onClose={() => setEditing(null)}>
        {editing !== null && <GoalForm goal={editing === 'new' ? undefined : editing} currency={user?.preferredCurrency ?? 'CAD'} onSaved={done} onCancel={() => setEditing(null)} />}
      </Modal>
      <Modal open={contributing !== null} title="Add contribution" onClose={() => setContributing(null)}>
        {contributing && <ContributionForm goal={contributing} onSaved={done} onCancel={() => setContributing(null)} />}
      </Modal>
      <ConfirmDialog
        open={deleting !== null}
        title="Delete goal?"
        message={`"${deleting?.name}" and its contribution history will be removed.`}
        confirmLabel="Delete goal"
        danger
        onConfirm={async () => {
          await goalService.remove(deleting!.id);
          goals.reload();
        }}
        onClose={() => setDeleting(null)}
      />
    </>
  );
}
