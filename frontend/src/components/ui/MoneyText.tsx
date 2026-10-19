import { formatMoney } from '../../utils/money';
import { cn } from '../../utils/cn';

interface MoneyTextProps {
  amount: number;
  currency: string;
  /** Colour positive green and negative red. */
  signed?: boolean;
  className?: string;
}

export function MoneyText({ amount, currency, signed = false, className }: MoneyTextProps) {
  return (
    <span
      className={cn(
        'tabular-nums',
        signed && amount > 0 && 'text-positive-600',
        signed && amount < 0 && 'text-negative-600',
        className,
      )}
    >
      {formatMoney(amount, currency)}
    </span>
  );
}
