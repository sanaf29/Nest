import { useEffect, useState } from 'react';
import { TransactionForm } from '../components/forms/TransactionForm';
import { TransferForm } from '../components/forms/TransferForm';
import { TransactionList } from '../components/TransactionList';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { ConfirmDialog } from '../components/ui/ConfirmDialog';
import { SelectField, TextField } from '../components/ui/Field';
import { Icon } from '../components/ui/Icon';
import { Modal } from '../components/ui/Modal';
import { PageHeader } from '../components/ui/PageHeader';
import { EmptyState, ErrorState, LoadingState } from '../components/ui/States';
import { useAsync } from '../hooks/useAsync';
import { useReferenceData } from '../hooks/useReferenceData';
import { transactionService, transferService } from '../services/transactionService';
import type { TransactionFilter, TransactionResponse, TransactionType } from '../types/api';

const PAGE_SIZE = 25;

/** Text input whose value is applied after the user pauses typing. */
function useDebounced<T>(value: T, ms = 350): T {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const timer = window.setTimeout(() => setDebounced(value), ms);
    return () => window.clearTimeout(timer);
  }, [value, ms]);
  return debounced;
}

export function TransactionsPage() {
  const reference = useReferenceData();
  const [filter, setFilter] = useState<TransactionFilter>({ sort: 'transactionDate,desc' });
  const [text, setText] = useState('');
  const q = useDebounced(text);
  const [page, setPage] = useState(0);
  const [dialog, setDialog] = useState<'expense' | 'transfer' | TransactionResponse | null>(null);
  const [deleting, setDeleting] = useState<TransactionResponse | null>(null);

  const results = useAsync(() => transactionService.search({ ...filter, q, page, size: PAGE_SIZE }), [JSON.stringify(filter), q, page]);
  const update = (patch: Partial<TransactionFilter>) => {
    setPage(0);
    setFilter((f) => ({ ...f, ...patch }));
  };
  const saved = () => {
    setDialog(null);
    results.reload();
  };
  const hasFilters = Boolean(q || filter.accountId || filter.categoryId || filter.type || filter.from || filter.to);

  return (
    <>
      <PageHeader
        title="Transactions"
        description="Everything you've recorded, newest first."
        actions={
          <>
            <Button variant="secondary" icon={<Icon name="arrows" className="h-4 w-4" />} onClick={() => setDialog('transfer')} disabled={reference.accounts.length < 2}>
              Transfer
            </Button>
            <Button icon={<Icon name="plus" className="h-4 w-4" />} onClick={() => setDialog('expense')} disabled={reference.accounts.length === 0}>
              Add transaction
            </Button>
          </>
        }
      />

      <Card className="mb-6" bodyClassName="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-6">
        <TextField label="Search" type="search" placeholder="Description" value={text} onChange={(e) => { setPage(0); setText(e.target.value); }} containerClassName="lg:col-span-2" />
        <SelectField label="Type" value={filter.type ?? ''} onChange={(e) => update({ type: (e.target.value || undefined) as TransactionType | undefined })}>
          <option value="">All</option>
          <option value="EXPENSE">Expenses</option>
          <option value="INCOME">Income</option>
          <option value="TRANSFER_OUT">Transfers</option>
        </SelectField>
        <SelectField label="Account" value={filter.accountId ?? ''} onChange={(e) => update({ accountId: e.target.value ? Number(e.target.value) : undefined })}>
          <option value="">All</option>
          {reference.accounts.map((a) => (
            <option key={a.id} value={a.id}>
              {a.name}
            </option>
          ))}
        </SelectField>
        <TextField label="From" type="date" value={filter.from ?? ''} onChange={(e) => update({ from: e.target.value || undefined })} />
        <TextField label="To" type="date" value={filter.to ?? ''} onChange={(e) => update({ to: e.target.value || undefined })} />
        <SelectField label="Category" value={filter.categoryId ?? ''} onChange={(e) => update({ categoryId: e.target.value ? Number(e.target.value) : undefined })} containerClassName="lg:col-span-2">
          <option value="">All</option>
          {reference.categories.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name} ({c.categoryType === 'INCOME' ? 'income' : 'expense'})
            </option>
          ))}
        </SelectField>
        <SelectField label="Sort" value={filter.sort} onChange={(e) => update({ sort: e.target.value })} containerClassName="lg:col-span-2">
          <option value="transactionDate,desc">Newest first</option>
          <option value="transactionDate,asc">Oldest first</option>
          <option value="amount,desc">Largest amount</option>
          <option value="amount,asc">Smallest amount</option>
        </SelectField>
        {hasFilters && (
          <div className="flex items-end lg:col-span-2">
            <Button variant="ghost" onClick={() => { setText(''); setPage(0); setFilter({ sort: filter.sort }); }}>
              Clear filters
            </Button>
          </div>
        )}
      </Card>

      <Card bodyClassName="p-0" title={results.data ? `${results.data.totalElements} transaction${results.data.totalElements === 1 ? '' : 's'}` : undefined}>
        {results.loading && !results.data ? (
          <LoadingState />
        ) : results.error ? (
          <div className="p-5">
            <ErrorState error={results.error} onRetry={results.reload} />
          </div>
        ) : results.data && results.data.content.length > 0 ? (
          <>
            <TransactionList transactions={results.data.content} onEdit={(t) => setDialog(t)} onDelete={setDeleting} />
            {results.data.totalPages > 1 && (
              <nav aria-label="Pagination" className="flex items-center justify-between border-t border-ink-100 px-5 py-3 text-sm">
                <Button variant="secondary" size="sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                  Previous
                </Button>
                <span className="text-ink-500">
                  Page {page + 1} of {results.data.totalPages}
                </span>
                <Button variant="secondary" size="sm" disabled={page + 1 >= results.data.totalPages} onClick={() => setPage((p) => p + 1)}>
                  Next
                </Button>
              </nav>
            )}
          </>
        ) : (
          <EmptyState title={hasFilters ? 'No matches' : 'No transactions yet'}>
            {hasFilters ? 'Try a different search or clear the filters.' : reference.accounts.length === 0 ? 'Add an account first, then record your income and expenses.' : 'Record your first income or expense.'}
          </EmptyState>
        )}
      </Card>

      <Modal open={dialog === 'expense' || (typeof dialog === 'object' && dialog !== null)} title={typeof dialog === 'object' && dialog ? 'Edit transaction' : 'Add transaction'} onClose={() => setDialog(null)}>
        {(dialog === 'expense' || (typeof dialog === 'object' && dialog)) && (
          <TransactionForm
            accounts={reference.accounts}
            categories={reference.categories}
            transaction={typeof dialog === 'object' && dialog ? dialog : undefined}
            onSaved={saved}
            onCancel={() => setDialog(null)}
          />
        )}
      </Modal>
      <Modal open={dialog === 'transfer'} title="Transfer between accounts" onClose={() => setDialog(null)}>
        {dialog === 'transfer' && <TransferForm accounts={reference.accounts} onSaved={saved} onCancel={() => setDialog(null)} />}
      </Modal>

      <ConfirmDialog
        open={deleting !== null}
        title={deleting?.transferId ? 'Delete transfer?' : 'Delete transaction?'}
        message={deleting?.transferId ? 'Both sides of this transfer will be removed and both account balances restored.' : 'This permanently removes the transaction and updates the account balance.'}
        confirmLabel="Delete"
        danger
        onConfirm={async () => {
          if (deleting!.transferId) await transferService.remove(deleting!.transferId);
          else await transactionService.remove(deleting!.id);
          results.reload();
        }}
        onClose={() => setDeleting(null)}
      />
    </>
  );
}
