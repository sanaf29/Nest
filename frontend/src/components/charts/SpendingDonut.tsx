import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts';
import type { SpendingSlice } from '../../utils/chartData';
import { formatMoney } from '../../utils/money';

/** Donut chart with a legend that doubles as the accessible data table. */
export function SpendingDonut({ slices, currency, total }: { slices: SpendingSlice[]; currency: string; total: number }) {
  return (
    <div className="grid grid-cols-1 items-center gap-6 sm:grid-cols-2">
      <div className="relative h-56" aria-hidden="true">
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie data={slices} dataKey="value" nameKey="name" innerRadius="62%" outerRadius="92%" paddingAngle={2} stroke="none">
              {slices.map((slice) => (
                <Cell key={slice.name} fill={slice.color} />
              ))}
            </Pie>
            <Tooltip formatter={(value) => formatMoney(Number(value), currency)} />
          </PieChart>
        </ResponsiveContainer>
        <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center">
          <span className="text-xs text-ink-500">Spent</span>
          <span className="text-lg font-semibold tabular-nums text-ink-900">{formatMoney(total, currency)}</span>
        </div>
      </div>
      <ul className="flex flex-col gap-2" aria-label="Spending by category">
        {slices.map((slice) => (
          <li key={slice.name} className="flex items-center justify-between gap-3 text-sm">
            <span className="flex min-w-0 items-center gap-2">
              <span className="h-2.5 w-2.5 shrink-0 rounded-full" style={{ backgroundColor: slice.color }} aria-hidden="true" />
              <span className="truncate text-ink-700">{slice.name}</span>
            </span>
            <span className="shrink-0 tabular-nums text-ink-900">
              {formatMoney(slice.value, currency)} <span className="text-ink-400">· {slice.percent.toFixed(0)}%</span>
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
}
