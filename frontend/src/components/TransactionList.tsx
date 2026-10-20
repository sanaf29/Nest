import { Badge } from './ui/Badge';
import { CategoryIcon } from './ui/CategoryIcon';
import { Icon } from './ui/Icon';
import { MoneyText } from './ui/MoneyText';
import type { CategoryResponse, TransactionResponse } from '../types/api';
import { formatDate } from '../utils/dates';
import { signedAmount, typeLabels } from '../utils/transactions';

interface TransactionListProps {
  transactions: TransactionResponse[];
  onEdit?: (t: TransactionResponse) => void;
  onDelete?: (t: TransactionResponse) => void;
  showAccount?: boolean;
  /** When given, icons use each category's colour. */
  categories?: CategoryResponse[];
}

const typeColors = { INCOME: '#83C9A5', EXPENSE: '#F28B88', TRANSFER_IN: '#90BFF2', TRANSFER_OUT: '#90BFF2' } as const;

/** Rows of transactions. Transfer entries are marked and can only be removed as a whole transfer. */
export function TransactionList({ transactions, onEdit, onDelete, showAccount = true, categories }: TransactionListProps) {
  const colors = new Map(categories?.map((c) => [c.id, c.color]));
  return (
    <ul className="flex flex-col">
      {transactions.map((t) => {
        const isTransfer = t.transferId !== null;
        const color = (t.categoryId !== null && colors.get(t.categoryId)) || typeColors[t.type];
        return (
          <li key={t.id} className="group flex items-center gap-3 rounded-2xl px-3 py-2.5 transition-colors hover:bg-ink-100/60 sm:px-4">
            <CategoryIcon name={t.categoryName} color={color} icon={isTransfer ? 'arrows' : undefined} />
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-semibold text-ink-900">{t.description ?? t.categoryName ?? typeLabels[t.type]}</p>
              <p className="mt-0.5 flex flex-wrap items-center gap-x-1.5 gap-y-1 text-xs text-ink-500">
                <span>{formatDate(t.transactionDate)}</span>
                {t.categoryName && <span>· {t.categoryName}</span>}
                {showAccount && <span>· {t.accountName}</span>}
                {isTransfer && <Badge tone="info">{typeLabels[t.type]}</Badge>}
              </p>
            </div>
            <MoneyText amount={signedAmount(t)} currency={t.currency} signed={!isTransfer} className="shrink-0 text-sm font-bold" />
            {(onEdit || onDelete) && (
              <div className="flex shrink-0 gap-0.5">
                {onEdit && !isTransfer && (
                  <button type="button" onClick={() => onEdit(t)} className="rounded-xl p-1.5 text-ink-400 hover:bg-ink-100 hover:text-ink-900" aria-label={`Edit ${t.description ?? 'transaction'}`}>
                    <Icon name="edit" className="h-4 w-4" />
                  </button>
                )}
                {onDelete && (
                  <button type="button" onClick={() => onDelete(t)} className="rounded-xl p-1.5 text-ink-400 hover:bg-ink-100 hover:text-ink-900" aria-label={`Delete ${t.description ?? 'transaction'}`}>
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
