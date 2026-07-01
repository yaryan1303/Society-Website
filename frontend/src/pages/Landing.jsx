import { Link } from 'react-router-dom';
import { Search, FileText, BarChart3, Zap, Database, Landmark, Sparkles, ArrowRight, LogIn } from 'lucide-react';
import Navbar from '../components/Navbar';
import Footer from '../components/Footer';
import MembersCarousel from '../components/MembersCarousel';
import { SOCIETY } from '../components/branding';

const COMMITMENTS = [
  { icon: Search, title: 'Full Transparency', text: 'Every share, deposit, loan and interest entry is recorded and visible to you — exactly as in the ledger book.' },
  { icon: FileText, title: 'Monthly Statements', text: 'See up-to-date balances any time, and know precisely how much interest is due this month.' },
  { icon: BarChart3, title: 'Regular Reports', text: 'Audit-ready records and Excel exports for the AGM, audits and your own record-keeping.' },
  { icon: Zap, title: 'Quick Grievance Resolution', text: 'A clear digital trail means questions are answered quickly and accurately.' },
  { icon: Database, title: 'Secure Digital Records', text: 'Your data is protected and backed up — no more lost or damaged paper pages.' },
  { icon: Landmark, title: 'Fair, Reducing-Balance Interest', text: 'Loan interest is charged only on what you still owe, so it falls as you repay.' },
];

export default function Landing() {
  return (
    <>
      <Navbar />
      <div className="motto-strip">{SOCIETY.motto.join('   •   ')}</div>

      <section className="hero">
        <div className="container">
          <span className="pill"><Sparkles size={15} /> The society ledger, now digital</span>
          <div className="hindi">{SOCIETY.hindi}</div>
          <h1>{SOCIETY.name}</h1>
          <p className="tagline">"{SOCIETY.tagline}"</p>
          <div className="row">
            <Link className="btn green lg" to="/login"><LogIn size={18} /> Member Login</Link>
            <a className="btn ghost lg" href="#committee">Meet the Committee <ArrowRight size={16} /></a>
          </div>
        </div>
      </section>

      <section className="section">
        <div className="container">
          <h2>Our commitment to members</h2>
          <p className="lead">
            We are digitising the society's personal ledger so every member can view their account
            from a phone or computer — the same trusted figures, now always accurate and up to date.
          </p>
          <div className="features">
            {COMMITMENTS.map((c) => (
              <div className="feature card hover" key={c.title}>
                <div className="ico"><c.icon /></div>
                <h3>{c.title}</h3>
                <p>{c.text}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="carousel-wrap section" id="committee">
        <div className="container">
          <h2>Our committee</h2>
          <p className="lead">The elected office-bearers and executive members serving the society.</p>
        </div>
        <MembersCarousel />
      </section>

      <section className="section center">
        <div className="container">
          <h2>Ready to check your account?</h2>
          <p className="lead" style={{ margin: '0 auto 1.5rem' }}>
            Log in with your ledger account number to view your shares, deposits, loan balance and interest due.
          </p>
          <Link className="btn green lg" to="/login">Go to Member Login <ArrowRight size={17} /></Link>
        </div>
      </section>

      <Footer />
    </>
  );
}
