import { Icon, type IconName } from './ui/Icon';
import type { InsightSeverity, InsightsResponse } from '../types/api';
import { cn } from '../utils/cn';

const styles: Record<InsightSeverity, { icon: IconName; tile: string; label: string }> = {
  ALERT: { icon: 'alert', tile: 'bg-negative-50 text-negative-600', label: 'Alert' },
  WARNING: { icon: 'alert', tile: 'bg-warning-50 text-warning-600', label: 'Heads up' },
  INFO: { icon: 'bulb', tile: 'bg-accent-100 text-accent-600', label: 'Insight' },
  POSITIVE: { icon: 'check', tile: 'bg-positive-50 text-positive-600', label: 'Nice work' },
};

export function InsightList({ insights, limit }: { insights: InsightsResponse['insights']; limit?: number }) {
  const shown = limit ? insights.slice(0, limit) : insights;
  return (
    <ul className="flex flex-col gap-3">
      {shown.map((insight) => {
        const style = styles[insight.severity];
        return (
          <li key={insight.code} className="flex animate-rise gap-3 rounded-2xl bg-surface-2 p-4 ring-1 ring-line/40">
            <span className={cn('flex h-9 w-9 shrink-0 items-center justify-center rounded-xl', style.tile)}>
              <Icon name={style.icon} className="h-5 w-5" />
            </span>
            <div className="min-w-0">
              <p className="text-[11px] font-bold tracking-widest text-ink-400 uppercase">{style.label}</p>
              <h3 className="font-semibold text-ink-900">{insight.title}</h3>
              <p className="mt-1 text-sm text-ink-600">{insight.message}</p>
            </div>
          </li>
        );
      })}
    </ul>
  );
}
