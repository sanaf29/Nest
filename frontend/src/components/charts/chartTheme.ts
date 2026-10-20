// Recharts accepts CSS variables in SVG attributes, so charts follow the active theme.
export const chartColors = {
  grid: 'var(--color-ink-100)',
  axis: 'var(--color-ink-400)',
  income: 'var(--color-positive-600)',
  expenses: 'var(--color-brand-600)',
  net: 'var(--color-accent-500)',
};

export const tooltipStyle = {
  contentStyle: {
    background: 'var(--color-surface-2)',
    border: '1px solid var(--color-line)',
    borderRadius: 14,
    color: 'var(--color-ink-900)',
    fontSize: 12,
    boxShadow: 'var(--shadow-lift)',
  },
  labelStyle: { color: 'var(--color-ink-500)', fontWeight: 600 },
  itemStyle: { color: 'var(--color-ink-900)' },
  cursor: { fill: 'var(--color-ink-100)', opacity: 0.5 },
};

export const compactNumber = new Intl.NumberFormat(undefined, { notation: 'compact', maximumFractionDigits: 1 });
