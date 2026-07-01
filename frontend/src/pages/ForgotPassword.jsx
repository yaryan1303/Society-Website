import { useState } from 'react';
import { Link } from 'react-router-dom';
import { User, AlertCircle, MailCheck, ArrowLeft } from 'lucide-react';
import AuthLayout from '../components/AuthLayout';
import { api } from '../api';
import { errMsg } from '../api/client';

export default function ForgotPassword() {
  const [accountNo, setAccountNo] = useState('');
  const [done, setDone] = useState(false);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function onSubmit(e) {
    e.preventDefault();
    setError('');
    setBusy(true);
    try {
      await api.forgotPassword(accountNo.trim());
      setDone(true);
    } catch (err) {
      setError(errMsg(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <AuthLayout>
      <div className="auth-card card pad">
        <h2 style={{ marginBottom: '.25rem' }}>Forgot password</h2>
        {done ? (
          <>
            <div style={{ width: 48, height: 48, borderRadius: 14, background: 'var(--success-soft)', color: 'var(--success-fg)', display: 'grid', placeItems: 'center', margin: '.5rem 0 1rem' }}>
              <MailCheck size={24} />
            </div>
            <div className="alert ok"><MailCheck size={18} />If an account with that number exists, a reset link has been sent to its registered email.</div>
            <Link className="btn ghost block" to="/login"><ArrowLeft size={16} /> Back to login</Link>
          </>
        ) : (
          <>
            <p className="muted" style={{ marginTop: 0 }}>Enter your account number and we'll email a secure reset link to the address on file.</p>
            {error && <div className="alert error"><AlertCircle size={18} />{error}</div>}
            <form onSubmit={onSubmit}>
              <div className="field">
                <label>Account Number</label>
                <div className="input-group">
                  <span className="lead-icon"><User /></span>
                  <input className="input" value={accountNo} autoFocus onChange={(e) => setAccountNo(e.target.value)} placeholder="e.g. 1583" />
                </div>
              </div>
              <button className="btn green block lg" disabled={busy || !accountNo}>
                {busy ? <span className="spinner" /> : 'Send reset link'}
              </button>
            </form>
            <div style={{ marginTop: '1.1rem', fontSize: '.9rem' }}><Link to="/login">← Back to login</Link></div>
          </>
        )}
      </div>
    </AuthLayout>
  );
}
