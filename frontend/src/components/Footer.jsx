import { SOCIETY } from './branding';

export default function Footer() {
  return (
    <footer className="footer">
      <div className="container">
        <div className="cols">
          <div>
            <h4>{SOCIETY.name}</h4>
            <p className="muted" style={{ color: '#cdd7ee' }}>{SOCIETY.hindi}</p>
            <p style={{ fontStyle: 'italic', maxWidth: 420 }}>{SOCIETY.tagline}</p>
          </div>
          <div>
            <h4>Our Values</h4>
            {SOCIETY.motto.map((m) => (
              <div key={m}>• {m}</div>
            ))}
          </div>
          <div>
            <h4>Members</h4>
            <div>Monthly statements</div>
            <div>Digital records</div>
            <div>Quick grievance resolution</div>
          </div>
        </div>
        <div className="bottom">
          © {new Date().getFullYear()} {SOCIETY.name}. For member use. All balances are system-computed and audit-ready.
        </div>
      </div>
    </footer>
  );
}
