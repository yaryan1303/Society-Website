import { Inbox } from 'lucide-react';

/** Shimmer skeleton block. */
export function Skeleton({ width, height, className = '', style }) {
  return <span className={`skeleton ${className}`} style={{ display: 'block', width, height, ...style }} />;
}

/** A table-like skeleton used while a panel/table loads. */
export function SkeletonRows({ rows = 4 }) {
  return (
    <div className="panel">
      <Skeleton className="sk-line" width="40%" height="1rem" />
      {Array.from({ length: rows }).map((_, i) => <Skeleton key={i} className="sk-row" />)}
    </div>
  );
}

/** Centered empty state with an icon, message and optional action. */
export function EmptyState({ icon: Icon = Inbox, title, message, action }) {
  return (
    <div className="empty-state">
      <div className="es-ico"><Icon /></div>
      {title && <h3>{title}</h3>}
      {message && <p className="muted" style={{ maxWidth: 420, margin: '0 auto' }}>{message}</p>}
      {action && <div style={{ marginTop: '1.1rem' }}>{action}</div>}
    </div>
  );
}

/** Lightweight CSS tooltip. Wrap any element: <Tooltip label="…"><button/></Tooltip> */
export function Tooltip({ label, children }) {
  return (
    <span className="tip" tabIndex={0}>
      {children}
      <span className="tip-pop" role="tooltip">{label}</span>
    </span>
  );
}

/** Dashboard KPI tile. */
export function StatCard({ icon: Icon, tone = '', label, value, sub }) {
  return (
    <div className="stat card">
      <div className="stat-top">
        {Icon && <div className={`ico ${tone}`}><Icon /></div>}
      </div>
      <div className="k">{label}</div>
      <div className="v">{value}</div>
      {sub && <div className="sub">{sub}</div>}
    </div>
  );
}
