import { Link } from 'react-router-dom';
import { ShieldCheck, TrendingUp, FileSpreadsheet } from 'lucide-react';
import { SOCIETY } from './branding';
import ThemeToggle from './ThemeToggle';

const POINTS = [
  { icon: ShieldCheck, text: 'Bank-grade security for every member record' },
  { icon: TrendingUp, text: 'Live balances for shares, deposits and loans' },
  { icon: FileSpreadsheet, text: 'Audit-ready statements, exactly like the ledger book' },
];

export default function AuthLayout({ children }) {
  return (
    <div className="auth-shell">
      <aside className="auth-aside">
        <Link to="/" className="brand" style={{ marginBottom: '2rem' }}>
          <span className="logo">ES</span>
          <span><div className="t1">{SOCIETY.short}</div><div className="t2">{SOCIETY.hindi}</div></span>
        </Link>
        <h2 style={{ color: '#fff', fontSize: '1.9rem', maxWidth: 420 }}>{SOCIETY.tagline}</h2>
        <div style={{ marginTop: '2rem', display: 'grid', gap: '1.1rem', maxWidth: 420 }}>
          {POINTS.map((p) => (
            <div key={p.text} style={{ display: 'flex', gap: '.85rem', alignItems: 'center' }}>
              <span style={{ flex: 'none', width: 38, height: 38, borderRadius: 11, display: 'grid', placeItems: 'center', background: 'rgba(255,255,255,.14)', color: '#fff' }}>
                <p.icon size={19} />
              </span>
              <span style={{ opacity: .92 }}>{p.text}</span>
            </div>
          ))}
        </div>
      </aside>
      <div className="auth-main">
        <div style={{ position: 'absolute', top: '1.1rem', right: '1.1rem' }}><ThemeToggle /></div>
        {children}
      </div>
    </div>
  );
}
