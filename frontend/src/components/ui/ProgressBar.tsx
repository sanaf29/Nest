import { cn } from '../../utils/cn';

type Tone = 'brand' | 'info' | 'positive' | 'warning' | 'negative';

const tones: Record<Tone, string> = {
  brand: 'bg-brand-600',
  info: 'bg-accent-500',
  positive: 'bg-positive-600',
  warning: 'bg-warning-600',
  negative: 'bg-negative-600',
};

interface ProgressBarProps {
  value: number;
  label: string;
  tone?: Tone;
  /** A custom fill colour (e.g. a category's pastel); overrides the tone. */
  color?: string;
  className?: string;
}

/** A thin labelled progress bar that grows in on mount. The bar caps at 100 %, the announced value does not. */
export function ProgressBar({ value, label, tone = 'brand', color, className }: ProgressBarProps) {
  const width = Math.max(0, Math.min(100, value));
  return (
    <div
      role="progressbar"
      aria-label={label}
      aria-valuenow={Math.round(value)}
      aria-valuemin={0}
      aria-valuemax={100}
      className={cn('h-1.5 w-full overflow-hidden rounded-full bg-ink-100', className)}
    >
      <div
        className={cn('h-full origin-left animate-grow rounded-full transition-[width] duration-500', !color && tones[tone])}
        style={{ width: `${width}%`, backgroundColor: color }}
      />
    </div>
  );
}

/** Tone for a budget line: coral when over, amber near the limit. */
export function budgetTone(percentUsed: number): Tone {
  return percentUsed > 100 ? 'negative' : percentUsed >= 80 ? 'warning' : 'positive';
}
