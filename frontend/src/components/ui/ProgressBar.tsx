import { cn } from '../../utils/cn';

type Tone = 'brand' | 'positive' | 'warning' | 'negative';

const tones: Record<Tone, string> = {
  brand: 'bg-brand-600',
  positive: 'bg-positive-600',
  warning: 'bg-accent-500',
  negative: 'bg-negative-600',
};

/** A labelled progress bar. The visible bar is capped at 100 %, the announced value is not. */
export function ProgressBar({ value, label, tone = 'brand', className }: { value: number; label: string; tone?: Tone; className?: string }) {
  const width = Math.max(0, Math.min(100, value));
  return (
    <div
      role="progressbar"
      aria-label={label}
      aria-valuenow={Math.round(value)}
      aria-valuemin={0}
      aria-valuemax={100}
      className={cn('h-2 w-full overflow-hidden rounded-full bg-ink-100', className)}
    >
      <div className={cn('h-full rounded-full transition-[width]', tones[tone])} style={{ width: `${width}%` }} />
    </div>
  );
}
