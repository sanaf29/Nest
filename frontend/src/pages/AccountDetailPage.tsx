import { useState } from 'react';
import { Link, useParams } from 'react-router';
import { accountTypeLabels } from '../components/forms/AccountForm';
import { TransactionList } from '../components/TransactionList';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { MoneyText } from '../components/ui/MoneyText';
import { EmptyState, ErrorState, LoadingState } from '../components/ui/States';
import { useAsync } from '../hooks/useAsync';
import { accountService } from '../services/accountService';
import { transactionService } from '../services/transactionService';

const PAGE_SIZE = 20;

export function AccountDetailPage() {
  const id = Number(useParams().id);
  const [page, setPage] = useState(0);
  const account = useAsync(() => accountService.get(id), [id]);
  const history = useAsync(() => transactionService.search({ accountId: id, page, size: PAGE_SIZE }), [id, page]);

  if (account.error) {
    return <ErrorState error={account.error} />;
  }
  if (!account.data) {
    return <LoadingState />;
  }
  const a = account.data;

  return (
    <>
      <Link to="/accounts" className="mb-4 inline-block text-sm text-brand-700 hover:underline">
        ← All accounts
      </Link>
      <div className="mb-6 flex flex-col gap-4 rounded-2xl bg-white p-6 shadow-sm ring-1 ring-ink-200/70 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-ink-900">{a.name}</h1>
          <div className="mt-2 flex gap-2">
            <Badge>{accountTypeLabels[a.accountType]}</Badge>
            <Badge tone="brand">{a.currency}</Badge>
            {!a.active && <Badge tone="warning">Closed</Badge>}
          </div>
        </div>
        <div className="sm:text-right">
          <p className="text-sm text-ink-500">Current balance</p>
          <p className="text-3xl font-semibold">
            <MoneyText amount={a.currentBalance} currency={a.currency} signed={a.currentBalance < 0} />
          </p>
          <p className="text-xs text-ink-500">
            Opening balance <MoneyText amount={a.openingBalance} currency={a.currency} />
          </p>
        </div>
      </div>

      <Card title="History" bodyClassName="p-0">
        {history.loading && !history.data ? (
          <LoadingState />
        ) : history.error ? (
          <div className="p-5">
            <ErrorState error={history.error} onRetry={history.reload} />
          </div>
        ) : history.data && history.data.content.length > 0 ? (
          <>
            <TransactionList transactions={history.data.content} showAccount={false} />
            {history.data.totalPages > 1 && (
              <div className="flex items-center justify-between border-t border-ink-100 px-5 py-3 text-sm">
                <Button variant="secondary" size="sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                  Previous
                </Button>
                <span className="text-ink-500">
                  Page {page + 1} of {history.data.totalPages}
                </span>
                <Button variant="secondary" size="sm" disabled={page + 1 >= history.data.totalPages} onClick={() => setPage((p) => p + 1)}>
                  Next
                </Button>
              </div>
            )}
          </>
        ) : (
          <EmptyState title="No activity yet">Income, expenses, and transfers on this account will appear here.</EmptyState>
        )}
      </Card>
    </>
  );
}
