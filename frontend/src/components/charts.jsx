import {
  ResponsiveContainer, PieChart, Pie, Cell, Tooltip, Legend,
  BarChart, Bar, XAxis, YAxis, CartesianGrid,
} from 'recharts';
import { money } from '../utils/format';

export const CHART_COLORS = ['#1b3a7a', '#0f766e', '#b45309', '#2a52c4', '#9333ea', '#0891b2'];

const tooltipStyle = {
  background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 10,
  color: 'var(--text)', boxShadow: 'var(--shadow-md)', fontSize: 13,
};

/** Donut with a centred total. data: [{ name, value }]. */
export function DonutChart({ data, colors = CHART_COLORS, height = 230, center }) {
  const total = data.reduce((s, d) => s + (Number(d.value) || 0), 0);
  if (!total) return null;
  return (
    <div style={{ position: 'relative' }}>
      <ResponsiveContainer width="100%" height={height}>
        <PieChart>
          <Pie data={data} dataKey="value" nameKey="name" innerRadius="60%" outerRadius="86%" paddingAngle={2} stroke="none">
            {data.map((_, i) => <Cell key={i} fill={colors[i % colors.length]} />)}
          </Pie>
          <Tooltip contentStyle={tooltipStyle} formatter={(v) => money(v)} />
          <Legend iconType="circle" wrapperStyle={{ fontSize: 13, color: 'var(--text-muted)' }} />
        </PieChart>
      </ResponsiveContainer>
      {center && (
        <div style={{ position: 'absolute', top: 'calc(50% - 22px)', left: 0, right: 0, textAlign: 'center', transform: 'translateY(-50%)', pointerEvents: 'none' }}>
          <div className="muted" style={{ fontSize: '.72rem', textTransform: 'uppercase', letterSpacing: '.04em' }}>{center.label}</div>
          <div style={{ fontSize: '1.15rem', fontWeight: 800, fontVariantNumeric: 'tabular-nums' }}>{center.value}</div>
        </div>
      )}
    </div>
  );
}

/** Vertical bars. data: [{ name, value }]. */
export function MiniBarChart({ data, color = '#1b3a7a', height = 230 }) {
  return (
    <ResponsiveContainer width="100%" height={height}>
      <BarChart data={data} margin={{ top: 8, right: 8, left: -8, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
        <XAxis dataKey="name" tick={{ fill: 'var(--text-muted)', fontSize: 12 }} axisLine={{ stroke: 'var(--border)' }} tickLine={false} />
        <YAxis tick={{ fill: 'var(--text-muted)', fontSize: 12 }} axisLine={false} tickLine={false} width={64}
               tickFormatter={(v) => (v >= 1000 ? `${(v / 1000).toFixed(0)}k` : v)} />
        <Tooltip contentStyle={tooltipStyle} cursor={{ fill: 'var(--surface-2)' }} formatter={(v) => money(v)} />
        <Bar dataKey="value" radius={[6, 6, 0, 0]} maxBarSize={48}>
          {data.map((d, i) => <Cell key={i} fill={d.color || color} />)}
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  );
}
