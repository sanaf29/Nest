import { useState } from 'react';
import { Link } from 'react-router';
import { AccountForm, accountTypeLabels } from '../components/forms/AccountForm';
import { TransferForm } from '../components/forms/TransferForm';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { ConfirmDialog } from '../components/ui/ConfirmDialog';
import { Icon, type IconName } from '../components/ui/Icon';
import { Modal } from '../components/ui/Modal';
import { MoneyText } from '../components/ui/MoneyText';
import { PageHeader } from '../components/ui/PageHeader';
import { EmptyState, ErrorState, LoadingState } from '../components/ui/States';
import { useAsync } from '../hooks/useAsync';
import { useAuth } from '../hooks/useAuth';
import { accountService } from '../services/accountService';
import type { AccountResponse, AccountType } from '../types/api';
import { cn } from '../utils/cn';
import { sumMoney } from '../utils/money';

const typeStyle: Record<AccountType, { icon: IconName; glow: string }> = {
  CHECKING: { icon: 'bank', glow: 'rgb(144 191 242 / 0.35)' },
  SAVINGS: { icon: 'piggy', glow: 'rgb(131 201 165 / 0.35)' },
  CASH: { icon: 'cash', glow: 'rgb(242 194 123 / 0.35)' },
  CREDIT_CARD: { icon: 'card', glow: 'rgb(242 139 136 / 0.4)' },
};

function AccountCard({ account, index, onEdit, onClose }: { account: AccountResponse; index: number; onEdit: () => void; onClose: () => void }) {
  const style = typeStyle[account.accountType];
  return (
    <li
      className={cn('card-hero relative flex min-h-44 animate-rise flex-col justify-between gap-4 overflow-hidden rounded-3xl p-5 shadow-card transition-transform duration-200 hover:-translate-y-1', !account.active && 'opacity-60 grayscale')}
      style={{ animationDelay: `${Math.min(index, 6) * 40}ms` }}
    >
      <div className="pointer-events-none absolute -top-12 -right-12 h-36 w-36 rounded-full blur-2xl" style={{ backgroundColor: style.glow }} aria-hidden="true" />
      <div className="relative flex items-start justify-between gap-3">
        <span className="flex h-11 w-11 items-center justify-center rounded-2xl bg-white/15 ring-1 ring-white/20" aria-hidden="true">
          <Icon name={style.icon} className="h-5.5 w-5.5" />
        </span>
        <div className="flex items-center gap-1.5 text-[11px] font-bold tracking-wide uppercase">
          <span className="rounded-full bg-white/15 px-2.5 py-1">{accountTypeLabels[account.accountType]}</span>
          <span className="rounded-full bg-black/20 px-2.5 py-1">{account.currency}</span>
          {!account.active && <span className="rounded-full bg-black/30 px-2.5 py-1">Closed</span>}
        </div>
      </div>
      <div className="relative">
        {/* The link's ::after makes the whole card open the account; the buttons sit above it. */}
        <Link to={`/accounts/${account.id}`} className="text-base font-semibold text-white/90 after:absolute after:inset-0 after:content-[''] hover:underline">
          {account.name}
        </Link>
        <div className="mt-1 flex items-end justify-between gap-3">
          <div>
            <MoneyText amount={account.currentBalance} currency={account.currency} className="text-3xl font-extrabold tracking-tight" />
            {account.accountType === 'CREDIT_CARD' && account.currentBalance < 0 && <p className="text-xs font-semibold text-white/75">owed</p>}
          </div>
          {account.active && (
            <div className="relative z-10 flex gap-1">
              <button type="button" onClick={onEdit} className="rounded-xl bg-white/10 p-2 text-white/85 hover:bg-white/20" aria-label={`Edit ${account.name}`}>
                <Icon name="edit" className="h-4 w-4" />
              </button>
              <button type="button" onClick={onClose} className="rounded-xl bg-white/10 p-2 text-white/85 hover:bg-white/20" aria-label={`Close ${account.name}`}>
                <Icon name="trash" className="h-4 w-4" />
              </button>
            </div>
          )}
        </div>
      </div>
    </li>
  );
}

export function AccountsPage() {
  const { user } = useAuth();
  const [showClosed, setShowClosed] = useState(false);
  const accounts = useAsync(() => accountService.list(showClosed), [showClosed]);
  const [editing, setEditing] = useState<AccountResponse | 'new' | null>(null);
  const [closing, setClosing] = useState<AccountResponse | null>(null);
  const [transferring, setTransferring] = useState(false);

  const list = accounts.data ?? [];
  const open = list.filter((a) => a.active);
  const currencies = [...new Set(open.map((a) => a.currency))].sort();

  return (
    <>
      <PageHeader
        title="Accounts"
        description="Balances are calculated from each account's opening balance and its activity."
        actions={
          <>
            <Button variant="secondary" icon={<Icon name="arrows" className="h-4 w-4" />} onClick={() => setTransferring(true)} disabled={open.length < 2}>
              Transfer
            </Button>
            <Button icon={<Icon name="plus" className="h-4 w-4" />} onClick={() => setEditing('new')}>
              Add account
            </Button>
          </>
        }
      />

      {currencies.length > 0 && (
        <div className="mb-6 grid grid-cols-1 gap-3 sm:grid-cols-3">
          {currencies.map((currency) => {
            const inCurrency = open.filter((a) => a.currency === currency);
            const total = sumMoney(inCurrency.map((a) => a.currentBalance), currency);
            return (
              <div key={currency} className="animate-rise rounded-3xl bg-surface p-5 shadow-card ring-1 ring-line/40">
                <p className="text-[11px] font-bold tracking-widest text-ink-400 uppercase">Net position · {currency}</p>
                <p className="mt-1 text-2xl font-bold tracking-tight">
                  <MoneyText amount={total} currency={currency} signed />
                </p>
                <p className="mt-0.5 text-xs text-ink-500">
                  {inCurrency.length} account{inCurrency.length === 1 ? '' : 's'}
                </p>
              </div>
            );
          })}
        </div>
      )}

      <div className="mb-3 flex items-center justify-between px-1">
        <h2 className="text-base font-semibold text-ink-900">Your accounts</h2>
        <label className="flex items-center gap-2 text-sm text-ink-500">
          <input type="checkbox" checked={showClosed} onChange={(e) => setShowClosed(e.target.checked)} className="h-4 w-4 rounded accent-brand-600" />
          Show closed
        </label>
      </div>

      {accounts.loading && !accounts.data ? (
        <LoadingState />
      ) : accounts.error ? (
        <ErrorState error={accounts.error} onRetry={accounts.reload} />
      ) : list.length === 0 ? (
        <Card>
          <EmptyState icon="wallet" title="No accounts yet" action={<Button onClick={() => setEditing('new')}>Add your first account</Button>}>
            Add your chequing, savings, cash, and credit card accounts to start tracking.
          </EmptyState>
        </Card>
      ) : (
        <ul className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {list.map((account, i) => (
            <AccountCard key={account.id} index={i} account={account} onEdit={() => setEditing(account)} onClose={() => setClosing(account)} />
          ))}
          <li>
            <button
              type="button"
              onClick={() => setEditing('new')}
              className="flex h-full min-h-44 w-full flex-col items-center justify-center gap-2 rounded-3xl border-2 border-dashed border-line text-sm font-semibold text-ink-500 transition-colors hover:border-brand-500 hover:text-brand-700"
            >
              <Icon name="plus" className="h-6 w-6" />
              New account
            </button>
          </li>
        </ul>
      )}

      <Modal open={editing !== null} title={editing === 'new' ? 'Add account' : 'Edit account'} onClose={() => setEditing(null)}>
        {editing !== null && (
          <AccountForm
            account={editing === 'new' ? undefined : editing}
            defaultCurrency={user?.preferredCurrency ?? 'CAD'}
            onSaved={() => {
              setEditing(null);
              accounts.reload();
            }}
            onCancel={() => setEditing(null)}
          />
        )}
      </Modal>

      <Modal open={transferring} title="Transfer between accounts" onClose={() => setTransferring(false)}>
        {transferring && (
          <TransferForm
            accounts={open}
            onSaved={() => {
              setTransferring(false);
              accounts.reload();
            }}
            onCancel={() => setTransferring(false)}
          />
        )}
      </Modal>

      <ConfirmDialog
        open={closing !== null}
        title="Close account?"
        message={`"${closing?.name}" will be hidden and no new transactions can be added. Its history stays in your reports.`}
        confirmLabel="Close account"
        danger
        onConfirm={async () => {
          await accountService.close(closing!.id);
          accounts.reload();
        }}
        onClose={() => setClosing(null)}
      />
    </>
  );
}
