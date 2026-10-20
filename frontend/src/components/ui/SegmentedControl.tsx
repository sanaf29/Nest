import { cn } from '../../utils/cn';

interface Option<T extends string> {
  value: T;
  label: string;
  /** Classes for the selected pill, e.g. a status colour. */
  activeClassName?: string;
}

/** A pill-shaped single choice (radio group), e.g. Expense / Income. */
export function SegmentedControl<T extends string>({ label, value, options, onChange, className }: { label: string; value: T; options: Option<T>[]; onChange: (value: T) => void; className?: string }) {
  return (
    <div role="radiogroup" aria-label={label} className={cn('grid gap-1 rounded-full bg-surface-2 p-1 ring-1 ring-inset ring-line/60', className)} style={{ gridTemplateColumns: `repeat(${options.length}, minmax(0, 1fr))` }}>
      {options.map((option) => (
        <button
          key={option.value}
          type="button"
          role="radio"
          aria-checked={value === option.value}
          onClick={() => onChange(option.value)}
          className={cn(
            'rounded-full px-3 py-2 text-sm font-semibold transition-colors',
            value === option.value ? (option.activeClassName ?? 'bg-brand-600 text-on-brand') : 'text-ink-500 hover:text-ink-900',
          )}
        >
          {option.label}
        </button>
      ))}
    </div>
  );
}
