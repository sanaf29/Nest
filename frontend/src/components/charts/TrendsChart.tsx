import { Bar, CartesianGrid, ComposedChart, Legend, Line, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { usePrefersReducedMotion } from '../../hooks/usePrefersReducedMotion';
import type { TrendRow } from '../../utils/chartData';
import { formatMoney } from '../../utils/money';

/** Income and expense bars per month with the net as a line. */
export function TrendsChart({ rows, currency }: { rows: TrendRow[]; currency: string }) {
  const animate = !usePrefersReducedMotion();
  const compact = new Intl.NumberFormat(undefined, { notation: 'compact', maximumFractionDigits: 1 });
  return (
    <div className="h-72" role="img" aria-label={`Income and expenses for the last ${rows.length} months`}>
      <ResponsiveContainer width="100%" height="100%">
        <ComposedChart data={rows} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
          <CartesianGrid vertical={false} stroke="#e2e8f0" />
          <XAxis dataKey="label" tickLine={false} axisLine={false} fontSize={12} stroke="#64748b" />
          <YAxis tickLine={false} axisLine={false} fontSize={12} stroke="#64748b" width={48} tickFormatter={(v: number) => compact.format(v)} />
          <Tooltip formatter={(value) => formatMoney(Number(value), currency)} />
          <Legend iconType="circle" wrapperStyle={{ fontSize: 12 }} />
          <Bar dataKey="income" name="Income" fill="#0d9488" radius={[4, 4, 0, 0]} maxBarSize={28} isAnimationActive={animate} />
          <Bar dataKey="expenses" name="Expenses" fill="#f59e0b" radius={[4, 4, 0, 0]} maxBarSize={28} isAnimationActive={animate} />
          <Line dataKey="net" name="Net" stroke="#1e293b" strokeWidth={2} dot={{ r: 3 }} type="monotone" isAnimationActive={animate} />
        </ComposedChart>
      </ResponsiveContainer>
    </div>
  );
}
