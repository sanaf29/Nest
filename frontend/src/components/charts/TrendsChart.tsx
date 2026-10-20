import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { usePrefersReducedMotion } from '../../hooks/usePrefersReducedMotion';
import type { TrendRow } from '../../utils/chartData';
import { formatMoney } from '../../utils/money';
import { chartColors, compactNumber, tooltipStyle } from './chartTheme';

/** Income and expense bars per month: the month-over-month comparison. */
export function TrendsChart({ rows, currency }: { rows: TrendRow[]; currency: string }) {
  const animate = !usePrefersReducedMotion();
  return (
    <div className="h-64" role="img" aria-label={`Income and expenses for the last ${rows.length} months`}>
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={rows} margin={{ top: 8, right: 4, left: -8, bottom: 0 }} barGap={4}>
          <CartesianGrid vertical={false} stroke={chartColors.grid} />
          <XAxis dataKey="label" tickLine={false} axisLine={false} fontSize={11} stroke={chartColors.axis} />
          <YAxis tickLine={false} axisLine={false} fontSize={11} stroke={chartColors.axis} width={44} tickFormatter={(v: number) => compactNumber.format(v)} />
          <Tooltip formatter={(value) => formatMoney(Number(value), currency)} {...tooltipStyle} />
          <Legend iconType="circle" iconSize={8} wrapperStyle={{ fontSize: 12, color: 'var(--color-ink-500)' }} />
          <Bar dataKey="income" name="Income" fill={chartColors.income} radius={[6, 6, 6, 6]} maxBarSize={16} isAnimationActive={animate} />
          <Bar dataKey="expenses" name="Expenses" fill={chartColors.expenses} radius={[6, 6, 6, 6]} maxBarSize={16} isAnimationActive={animate} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
