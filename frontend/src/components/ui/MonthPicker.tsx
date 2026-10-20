import { addMonths, monthLabel, type YearMonth } from '../../utils/dates';
import { Icon } from './Icon';

export function MonthPicker({ value, onChange }: { value: YearMonth; onChange: (value: YearMonth) => void }) {
  return (
    <div className="inline-flex items-center gap-1 rounded-full bg-surface p-1 ring-1 ring-line/50">
      <button type="button" onClick={() => onChange(addMonths(value, -1))} className="rounded-full p-1.5 text-ink-500 hover:bg-ink-100 hover:text-ink-900" aria-label="Previous month">
        <Icon name="chevronLeft" className="h-4 w-4" />
      </button>
      <span className="min-w-32 text-center text-sm font-semibold text-ink-800" aria-live="polite">
        {monthLabel(value)}
      </span>
      <button type="button" onClick={() => onChange(addMonths(value, 1))} className="rounded-full p-1.5 text-ink-500 hover:bg-ink-100 hover:text-ink-900" aria-label="Next month">
        <Icon name="chevronRight" className="h-4 w-4" />
      </button>
    </div>
  );
}
