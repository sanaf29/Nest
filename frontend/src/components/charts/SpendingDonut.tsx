import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts';
import { usePrefersReducedMotion } from '../../hooks/usePrefersReducedMotion';
import type { SpendingSlice } from '../../utils/chartData';
import { pastel } from '../../utils/categoryStyle';
import { formatMoney } from '../../utils/money';
import { tooltipStyle } from './chartTheme';

/** Donut chart with a legend that doubles as the accessible data table. */
export function SpendingDonut({ slices, currency, total }: { slices: SpendingSlice[]; currency: string; total: number }) {
  const animate = !usePrefersReducedMotion();
  return (
    <div className="grid grid-cols-1 items-center gap-6 sm:grid-cols-2">
      <div className="relative mx-auto h-56 w-full max-w-64" aria-hidden="true">
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie data={slices} dataKey="value" nameKey="name" innerRadius="68%" outerRadius="94%" paddingAngle={3} cornerRadius={6} stroke="none" isAnimationActive={animate}>
              {slices.map((slice) => (
                <Cell key={slice.name} fill={pastel(slice.color)} />
              ))}
            </Pie>
            <Tooltip formatter={(value) => formatMoney(Number(value), currency)} {...tooltipStyle} />
          </PieChart>
        </ResponsiveContainer>
        <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center">
          <span className="text-xs text-ink-500">Spent</span>
          <span className="text-lg font-bold tabular-nums text-ink-900">{formatMoney(total, currency)}</span>
        </div>
      </div>
      <ul className="flex flex-col gap-2.5" aria-label="Spending by category">
        {slices.map((slice) => (
          <li key={slice.name} className="flex items-center justify-between gap-3 text-sm">
            <span className="flex min-w-0 items-center gap-2">
              <span className="h-2.5 w-2.5 shrink-0 rounded-full" style={{ backgroundColor: pastel(slice.color) }} aria-hidden="true" />
              <span className="truncate text-ink-700">{slice.name}</span>
            </span>
            <span className="shrink-0 font-semibold tabular-nums text-ink-900">
              {formatMoney(slice.value, currency)} <span className="font-normal text-ink-400">· {slice.percent.toFixed(0)}%</span>
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
}
