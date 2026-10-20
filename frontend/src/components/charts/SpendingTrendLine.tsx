import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { usePrefersReducedMotion } from '../../hooks/usePrefersReducedMotion';
import type { TrendRow } from '../../utils/chartData';
import { formatMoney } from '../../utils/money';
import { chartColors, compactNumber, tooltipStyle } from './chartTheme';

/** Monthly spending as a soft line with a gradient fill. */
export function SpendingTrendLine({ rows, currency }: { rows: TrendRow[]; currency: string }) {
  const animate = !usePrefersReducedMotion();
  return (
    <div className="h-48" role="img" aria-label={`Spending over the last ${rows.length} months`}>
      <ResponsiveContainer width="100%" height="100%">
        <AreaChart data={rows} margin={{ top: 8, right: 4, left: -8, bottom: 0 }}>
          <defs>
            <linearGradient id="spendFill" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="var(--color-brand-600)" stopOpacity={0.35} />
              <stop offset="100%" stopColor="var(--color-brand-600)" stopOpacity={0} />
            </linearGradient>
          </defs>
          <CartesianGrid vertical={false} stroke={chartColors.grid} />
          <XAxis dataKey="label" tickLine={false} axisLine={false} fontSize={11} stroke={chartColors.axis} />
          <YAxis tickLine={false} axisLine={false} fontSize={11} stroke={chartColors.axis} width={44} tickFormatter={(v: number) => compactNumber.format(v)} />
          <Tooltip formatter={(value) => formatMoney(Number(value), currency)} {...tooltipStyle} />
          <Area
            dataKey="expenses"
            name="Spending"
            type="monotone"
            stroke={chartColors.expenses}
            strokeWidth={2.5}
            fill="url(#spendFill)"
            dot={{ r: 3, fill: 'var(--color-surface)', strokeWidth: 2 }}
            activeDot={{ r: 5 }}
            isAnimationActive={animate}
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
}
