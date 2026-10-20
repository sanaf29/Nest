import type { ReactNode } from 'react';
import { cn } from '../../utils/cn';

type Tone = 'neutral' | 'brand' | 'info' | 'positive' | 'warning' | 'negative';

const tones: Record<Tone, string> = {
  neutral: 'bg-ink-100 text-ink-600',
  brand: 'bg-brand-50 text-brand-700',
  info: 'bg-accent-100 text-accent-600',
  positive: 'bg-positive-50 text-positive-600',
  warning: 'bg-warning-50 text-warning-600',
  negative: 'bg-negative-50 text-negative-600',
};

export function Badge({ tone = 'neutral', children, className }: { tone?: Tone; children: ReactNode; className?: string }) {
  return (
    <span className={cn('inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-semibold', tones[tone], className)}>
      {children}
    </span>
  );
}
