import { Icon, type IconName } from './ui/Icon';
import type { InsightSeverity, InsightsResponse } from '../types/api';
import { cn } from '../utils/cn';

const styles: Record<InsightSeverity, { icon: IconName; box: string; iconColor: string; label: string }> = {
  ALERT: { icon: 'alert', box: 'bg-negative-50 ring-red-200', iconColor: 'text-negative-600', label: 'Alert' },
  WARNING: { icon: 'alert', box: 'bg-warning-50 ring-amber-200', iconColor: 'text-warning-600', label: 'Heads up' },
  INFO: { icon: 'bulb', box: 'bg-brand-50 ring-brand-200', iconColor: 'text-brand-700', label: 'Insight' },
  POSITIVE: { icon: 'check', box: 'bg-positive-50 ring-emerald-200', iconColor: 'text-positive-600', label: 'Nice work' },
};

export function InsightList({ insights, limit }: { insights: InsightsResponse['insights']; limit?: number }) {
  const shown = limit ? insights.slice(0, limit) : insights;
  return (
    <ul className="flex flex-col gap-3">
      {shown.map((insight) => {
        const style = styles[insight.severity];
        return (
          <li key={insight.code} className={cn('flex gap-3 rounded-xl p-4 ring-1', style.box)}>
            <Icon name={style.icon} className={cn('mt-0.5 h-5 w-5 shrink-0', style.iconColor)} />
            <div className="min-w-0">
              <p className="text-xs font-semibold tracking-wide text-ink-500 uppercase">{style.label}</p>
              <h3 className="font-semibold text-ink-900">{insight.title}</h3>
              <p className="mt-1 text-sm text-ink-700">{insight.message}</p>
            </div>
          </li>
        );
      })}
    </ul>
  );
}
