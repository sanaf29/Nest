import { Badge } from './ui/Badge';
import { Icon } from './ui/Icon';
import { MoneyText } from './ui/MoneyText';
import type { TransactionResponse } from '../types/api';
import { formatDate } from '../utils/dates';
import { signedAmount, typeLabels } from '../utils/transactions';

interface TransactionListProps {
  transactions: TransactionResponse[];
  onEdit?: (t: TransactionResponse) => void;
  onDelete?: (t: TransactionResponse) => void;
  showAccount?: boolean;
}

/** Rows of transactions. Transfer entries are marked and can only be removed as a whole transfer. */
export function TransactionList({ transactions, onEdit, onDelete, showAccount = true }: TransactionListProps) {
  return (
    <ul className="divide-y divide-ink-100">
      {transactions.map((t) => {
        const isTransfer = t.transferId !== null;
        return (
          <li key={t.id} className="flex items-center gap-3 px-5 py-3">
            <div className={`hidden h-9 w-9 shrink-0 items-center justify-center rounded-full sm:flex ${isTransfer ? 'bg-ink-100 text-ink-600' : t.type === 'INCOME' ? 'bg-positive-50 text-positive-600' : 'bg-accent-100 text-accent-600'}`}>
              <Icon name={isTransfer ? 'arrows' : t.type === 'INCOME' ? 'plus' : 'wallet'} className="h-4 w-4" />
            </div>
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-medium text-ink-900">{t.description ?? t.categoryName ?? typeLabels[t.type]}</p>
              <p className="mt-0.5 flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-ink-500">
                <span>{formatDate(t.transactionDate)}</span>
                {t.categoryName && <span>· {t.categoryName}</span>}
                {showAccount && <span>· {t.accountName}</span>}
                {isTransfer && <Badge>{typeLabels[t.type]}</Badge>}
              </p>
            </div>
            <MoneyText amount={signedAmount(t)} currency={t.currency} signed={!isTransfer} className="shrink-0 text-sm font-semibold" />
            {(onEdit || onDelete) && (
              <div className="flex shrink-0 gap-1">
                {onEdit && !isTransfer && (
                  <button type="button" onClick={() => onEdit(t)} className="rounded-lg p-1.5 text-ink-500 hover:bg-ink-100" aria-label={`Edit ${t.description ?? 'transaction'}`}>
                    <Icon name="edit" className="h-4 w-4" />
                  </button>
                )}
                {onDelete && (
                  <button type="button" onClick={() => onDelete(t)} className="rounded-lg p-1.5 text-ink-500 hover:bg-ink-100" aria-label={`Delete ${t.description ?? 'transaction'}`}>
                    <Icon name="trash" className="h-4 w-4" />
                  </button>
                )}
              </div>
            )}
          </li>
        );
      })}
    </ul>
  );
}
