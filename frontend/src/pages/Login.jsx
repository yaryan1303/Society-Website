import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { User, AlertCircle, ArrowRight } from 'lucide-react';
import AuthLayout from '../components/AuthLayout';
import PasswordInput from '../components/ui/PasswordInput';
import { useAuth } from '../auth/AuthContext';
import { errMsg } from '../api/client';
import { SOCIETY } from '../components/branding';

export default function Login() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const [accountNo, setAccountNo] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  if (user) {
    if (user.mustChangePassword) return <Navigate to="/first-login" replace />;
    return <Navigate to={user.role === 'ADMIN' ? '/admin' : '/me'} replace />;
  }

  async function onSubmit(e) {
    e.preventDefault();
    setError('');
    setBusy(true);
    try {
      const u = await login(accountNo.trim(), password);
      navigate(u.mustChangePassword ? '/first-login' : u.role === 'ADMIN' ? '/admin' : '/me');
    } catch (err) {
      setError(errMsg(err, 'Login failed'));
    } finally {
      setBusy(false);
    }
  }

  return (
    <AuthLayout>
      <div className="auth-card card pad">
        <h2 style={{ marginBottom: '.25rem' }}>Welcome back</h2>
        <p className="muted" style={{ marginTop: 0 }}>Sign in to your {SOCIETY.short} account.</p>
        {error && <div className="alert error"><AlertCircle size={18} />{error}</div>}
        <form onSubmit={onSubmit}>
          <div className="field">
            <label htmlFor="acc">Account Number</label>
            <div className="input-group">
              <span className="lead-icon"><User /></span>
              <input id="acc" className="input" value={accountNo} autoFocus
                     onChange={(e) => setAccountNo(e.target.value)} placeholder="e.g. 1583" />
            </div>
          </div>
          <div className="field">
            <label htmlFor="pwd">Password</label>
            <PasswordInput id="pwd" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="Your password" />
          </div>
          <button className="btn green block lg" disabled={busy || !accountNo || !password}>
            {busy ? <span className="spinner" /> : <>Log in <ArrowRight size={17} /></>}
          </button>
        </form>
        <div className="spread" style={{ marginTop: '1.1rem', fontSize: '.9rem' }}>
          <Link to="/forgot">Forgot password?</Link>
          <Link to="/">← Back to home</Link>
        </div>
      </div>
    </AuthLayout>
  );
}
