import type { ReactNode } from 'react';
import { cn } from '../../utils/cn';

type Tone = 'neutral' | 'brand' | 'positive' | 'warning' | 'negative';

const tones: Record<Tone, string> = {
  neutral: 'bg-ink-100 text-ink-700',
  brand: 'bg-brand-50 text-brand-800',
  positive: 'bg-positive-50 text-positive-600',
  warning: 'bg-warning-50 text-warning-600',
  negative: 'bg-negative-50 text-negative-600',
};

export function Badge({ tone = 'neutral', children, className }: { tone?: Tone; children: ReactNode; className?: string }) {
  return (
    <span className={cn('inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium', tones[tone], className)}>
      {children}
    </span>
  );
}
