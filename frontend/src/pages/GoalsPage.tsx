import { useEffect, useState } from 'react';
import { ContributionForm, GoalForm } from '../components/forms/GoalForm';
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
import { goalService } from '../services/goalService';
import type { SavingsGoalResponse } from '../types/api';
import { goalIcon } from '../utils/categoryStyle';
import { cn } from '../utils/cn';
import { formatDate, todayIso } from '../utils/dates';
import { formatMoney, sumMoney } from '../utils/money';

const MILESTONES = [25, 50, 75, 100] as const;

/** The highest milestone passed when progress moved from `before` to `after`, if any. */
export function crossedMilestone(before: number, after: number): number | null {
  return [...MILESTONES].reverse().find((m) => before < m && after >= m) ?? null;
}

/** "3 months left", "12 days left", or "Past target date". */
function timeLeft(targetDate: string): string {
  const [y, m, d] = targetDate.split('-').map(Number);
  const [ty, tm, td] = todayIso().split('-').map(Number);
  const days = Math.round((Date.UTC(y!, m! - 1, d!) - Date.UTC(ty!, tm! - 1, td!)) / 864e5);
  if (days < 0) return 'Past target date';
  if (days < 60) return `${days} day${days === 1 ? '' : 's'} left`;
  const months = Math.round(days / 30.4);
  return `${months} months left`;
}

function GoalCard({ goal, onContribute, onEdit, onDelete, index }: { goal: SavingsGoalResponse; onContribute: () => void; onEdit: () => void; onDelete: () => void; index: number }) {
  const [showHistory, setShowHistory] = useState(false);
  const achieved = goal.status === 'ACHIEVED';
  return (
    <article className="flex animate-rise flex-col overflow-hidden rounded-3xl bg-surface shadow-card ring-1 ring-line/40 transition-transform duration-200 hover:-translate-y-0.5" style={{ animationDelay: `${Math.min(index, 6) * 40}ms` }}>
      <div className="card-hero relative p-5">
        <div className="flex items-start justify-between gap-3">
          <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-white/15 ring-1 ring-white/20" aria-hidden="true">
            <Icon name={goalIcon(goal.name)} className="h-6 w-6" />
          </span>
          {achieved ? (
            <span className="inline-flex animate-pop items-center gap-1 rounded-full bg-positive-600 px-2.5 py-1 text-xs font-bold text-on-brand">
              <Icon name="sparkle" className="h-3.5 w-3.5" />
              Achieved
            </span>
          ) : (
            <span className="rounded-full bg-white/15 px-2.5 py-1 text-xs font-bold">{goal.progressPercent.toFixed(0)}%</span>
          )}
        </div>
        <h3 className="mt-4 truncate text-lg font-bold">{goal.name}</h3>
        <p className="text-xs text-white/75">
          {goal.targetDate ? `By ${formatDate(goal.targetDate)}${achieved ? '' : ` · ${timeLeft(goal.targetDate)}`}` : 'No deadline'}
        </p>
        <p className="mt-3 text-2xl font-extrabold tabular-nums">
          <MoneyText amount={goal.savedAmount} currency={goal.currency} />
          <span className="text-sm font-medium text-white/70"> of {formatMoney(goal.targetAmount, goal.currency)}</span>
        </p>
        <div className="mt-3 h-2 overflow-hidden rounded-full bg-white/20">
          <div
            role="progressbar"
            aria-label={`${goal.name} progress`}
            aria-valuenow={Math.round(goal.progressPercent)}
            aria-valuemin={0}
            aria-valuemax={100}
            className="h-full origin-left animate-grow rounded-full bg-positive-600"
            style={{ width: `${Math.min(100, goal.progressPercent)}%` }}
          />
        </div>
        <ol className="mt-3 flex justify-between" aria-label="Milestones">
          {MILESTONES.map((m) => {
            const reached = goal.progressPercent >= m;
            return (
              <li key={m} className={cn('flex items-center gap-1 text-[11px] font-semibold', reached ? 'text-white' : 'text-white/50')}>
                <span className={cn('h-1.5 w-1.5 rounded-full', reached ? 'bg-positive-600' : 'bg-white/30')} aria-hidden="true" />
                {m}%<span className="sr-only">{reached ? ' reached' : ' not yet'}</span>
              </li>
            );
          })}
        </ol>
      </div>
      <div className="flex flex-1 flex-col gap-3 p-4">
        {!achieved && (
          <p className="text-sm text-ink-500">
            <MoneyText amount={goal.remainingAmount} currency={goal.currency} className="font-semibold text-ink-900" /> to go
          </p>
        )}
        <div className="flex flex-wrap items-center gap-1.5">
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
            <Button size="sm" variant="ghost" onClick={() => setShowHistory((s) => !s)} aria-expanded={showHistory} className="ml-auto">
              {showHistory ? 'Hide' : 'Show'} {goal.contributions.length} contribution{goal.contributions.length === 1 ? '' : 's'}
            </Button>
          )}
        </div>
        {showHistory && (
          <ul className="animate-rise divide-y divide-ink-100 rounded-2xl bg-surface-2 text-sm ring-1 ring-line/40">
            {goal.contributions.map((c) => (
              <li key={c.id} className="flex justify-between gap-2 px-3 py-2">
                <span className="text-ink-600">
                  {formatDate(c.contributionDate)}
                  {c.note && ` · ${c.note}`}
                </span>
                <MoneyText amount={c.amount} currency={goal.currency} className="font-semibold text-positive-600" />
              </li>
            ))}
          </ul>
        )}
      </div>
    </article>
  );
}

export function GoalsPage() {
  const { user } = useAuth();
  const currency = user?.preferredCurrency ?? 'CAD';
  const goals = useAsync(() => goalService.list(), []);
  const [editing, setEditing] = useState<SavingsGoalResponse | 'new' | null>(null);
  const [contributing, setContributing] = useState<SavingsGoalResponse | null>(null);
  const [deleting, setDeleting] = useState<SavingsGoalResponse | null>(null);
  const [pending, setPending] = useState<{ id: number; before: number; stale: SavingsGoalResponse[] | undefined } | null>(null);
  const [celebration, setCelebration] = useState<string | null>(null);

  // After a contribution, once fresh goals arrive, celebrate (quietly) if it crossed a milestone.
  useEffect(() => {
    if (!pending || goals.loading || goals.data === pending.stale) return;
    const goal = goals.data?.find((g) => g.id === pending.id);
    const milestone = goal ? crossedMilestone(pending.before, goal.progressPercent) : null;
    setPending(null);
    if (goal && milestone) {
      setCelebration(milestone === 100 ? `You reached your ${goal.name} goal!` : `${goal.name} just passed ${milestone}%. Nice momentum!`);
    }
  }, [goals.data, goals.loading, pending]);

  useEffect(() => {
    if (!celebration) return;
    const timer = window.setTimeout(() => setCelebration(null), 6000);
    return () => window.clearTimeout(timer);
  }, [celebration]);

  const active = goals.data?.filter((g) => g.status === 'ACTIVE') ?? [];
  const achieved = goals.data?.filter((g) => g.status === 'ACHIEVED') ?? [];
  const inCurrency = active.filter((g) => g.currency === currency);
  const saved = sumMoney(inCurrency.map((g) => g.savedAmount), currency);
  const target = sumMoney(inCurrency.map((g) => g.targetAmount), currency);

  const cards = (list: SavingsGoalResponse[]) => (
    <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
      {list.map((g, i) => (
        <GoalCard key={g.id} index={i} goal={g} onContribute={() => setContributing(g)} onEdit={() => setEditing(g)} onDelete={() => setDeleting(g)} />
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

      {celebration && (
        <div role="status" className="mb-5 flex animate-pop items-center gap-3 rounded-2xl bg-positive-50 px-4 py-3 text-sm font-semibold text-positive-600 ring-1 ring-positive-600/30">
          <Icon name="sparkle" className="h-5 w-5 shrink-0" />
          {celebration}
        </div>
      )}

      {goals.loading && !goals.data ? (
        <LoadingState />
      ) : goals.error ? (
        <ErrorState error={goals.error} onRetry={goals.reload} />
      ) : goals.data?.length === 0 ? (
        <Card>
          <EmptyState icon="target" title="No goals yet" action={<Button onClick={() => setEditing('new')}>Create your first goal</Button>}>
            An emergency fund, a new laptop, a vacation, tuition: give your savings a purpose.
          </EmptyState>
        </Card>
      ) : (
        <div className="flex flex-col gap-8">
          {inCurrency.length > 0 && (
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
              {[
                ['Saved', formatMoney(saved, currency)],
                ['Target', formatMoney(target, currency)],
                ['Active goals', String(active.length)],
              ].map(([label, value]) => (
                <div key={label} className="min-w-0 rounded-3xl last:col-span-2 sm:last:col-span-1 bg-surface p-4 shadow-card ring-1 ring-line/40">
                  <p className="text-[11px] font-bold tracking-widest text-ink-400 uppercase">{label}</p>
                  <p className="mt-1 truncate text-lg font-bold text-ink-900 tabular-nums">{value}</p>
                </div>
              ))}
            </div>
          )}
          {active.length > 0 && cards(active)}
          {achieved.length > 0 && (
            <section>
              <h2 className="mb-3 flex items-center gap-2 px-1 text-sm font-bold tracking-widest text-ink-400 uppercase">
                Achieved <Badge tone="positive">{achieved.length}</Badge>
              </h2>
              {cards(achieved)}
            </section>
          )}
        </div>
      )}

      <Modal open={editing !== null} title={editing === 'new' ? 'New savings goal' : 'Edit goal'} onClose={() => setEditing(null)}>
        {editing !== null && <GoalForm goal={editing === 'new' ? undefined : editing} currency={currency} onSaved={done} onCancel={() => setEditing(null)} />}
      </Modal>
      <Modal open={contributing !== null} title="Add contribution" onClose={() => setContributing(null)}>
        {contributing && (
          <ContributionForm
            goal={contributing}
            onSaved={() => {
              setPending({ id: contributing.id, before: contributing.progressPercent, stale: goals.data });
              done();
            }}
            onCancel={() => setContributing(null)}
          />
        )}
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
