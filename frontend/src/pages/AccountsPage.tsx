import { useState } from 'react';
import { Link } from 'react-router';
import { AccountForm, accountTypeLabels } from '../components/forms/AccountForm';
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
import { accountService } from '../services/accountService';
import type { AccountResponse } from '../types/api';
import { sumMoney } from '../utils/money';

export function AccountsPage() {
  const { user } = useAuth();
  const [showClosed, setShowClosed] = useState(false);
  const accounts = useAsync(() => accountService.list(showClosed), [showClosed]);
  const [editing, setEditing] = useState<AccountResponse | 'new' | null>(null);
  const [closing, setClosing] = useState<AccountResponse | null>(null);

  const list = accounts.data ?? [];
  const currencies = [...new Set(list.filter((a) => a.active).map((a) => a.currency))].sort();

  return (
    <>
      <PageHeader
        title="Accounts"
        description="Balances are calculated from each account's opening balance and its activity."
        actions={
          <Button icon={<Icon name="plus" className="h-4 w-4" />} onClick={() => setEditing('new')}>
            Add account
          </Button>
        }
      />

      {currencies.length > 0 && (
        <div className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
          {currencies.map((currency) => (
            <div key={currency} className="rounded-2xl bg-white p-5 shadow-sm ring-1 ring-ink-200/70">
              <p className="text-sm text-ink-500">Net position · {currency}</p>
              <p className="mt-1 text-2xl font-semibold">
                <MoneyText amount={sumMoney(list.filter((a) => a.active && a.currency === currency).map((a) => a.currentBalance), currency)} currency={currency} signed />
              </p>
            </div>
          ))}
        </div>
      )}

      <Card
        title="Your accounts"
        action={
          <label className="flex items-center gap-2 text-sm text-ink-600">
            <input type="checkbox" checked={showClosed} onChange={(e) => setShowClosed(e.target.checked)} className="rounded border-ink-300 text-brand-700" />
            Show closed
          </label>
        }
        bodyClassName="p-0"
      >
        {accounts.loading && !accounts.data ? (
          <LoadingState />
        ) : accounts.error ? (
          <div className="p-5">
            <ErrorState error={accounts.error} onRetry={accounts.reload} />
          </div>
        ) : list.length === 0 ? (
          <EmptyState icon="wallet" title="No accounts yet" action={<Button onClick={() => setEditing('new')}>Add your first account</Button>}>
            Add your chequing, savings, cash, and credit card accounts to start tracking.
          </EmptyState>
        ) : (
          <ul className="divide-y divide-ink-100">
            {list.map((account) => (
              <li key={account.id} className="flex flex-col gap-3 px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
                <div className="min-w-0">
                  <Link to={`/accounts/${account.id}`} className="font-medium text-ink-900 hover:text-brand-700 hover:underline">
                    {account.name}
                  </Link>
                  <div className="mt-1 flex flex-wrap items-center gap-2 text-xs text-ink-500">
                    <Badge>{accountTypeLabels[account.accountType]}</Badge>
                    <span>{account.currency}</span>
                    {!account.active && <Badge tone="warning">Closed</Badge>}
                  </div>
                </div>
                <div className="flex items-center justify-between gap-4 sm:justify-end">
                  <div className="text-right">
                    <MoneyText amount={account.currentBalance} currency={account.currency} signed={account.currentBalance < 0} className="text-lg font-semibold" />
                    {account.accountType === 'CREDIT_CARD' && account.currentBalance < 0 && <p className="text-xs text-ink-500">owed</p>}
                  </div>
                  {account.active && (
                    <div className="flex gap-1">
                      <Button variant="ghost" size="sm" onClick={() => setEditing(account)} aria-label={`Edit ${account.name}`}>
                        <Icon name="edit" className="h-4 w-4" />
                      </Button>
                      <Button variant="ghost" size="sm" onClick={() => setClosing(account)} aria-label={`Close ${account.name}`}>
                        <Icon name="trash" className="h-4 w-4" />
                      </Button>
                    </div>
                  )}
                </div>
              </li>
            ))}
          </ul>
        )}
      </Card>

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
