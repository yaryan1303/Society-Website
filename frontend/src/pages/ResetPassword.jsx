import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { AlertCircle, CheckCircle2 } from 'lucide-react';
import AuthLayout from '../components/AuthLayout';
import PasswordInput from '../components/ui/PasswordInput';
import { api } from '../api';
import { errMsg } from '../api/client';

export default function ResetPassword() {
  const [params] = useSearchParams();
  const token = params.get('token') || '';
  const [newPassword, setNew] = useState('');
  const [confirm, setConfirm] = useState('');
  const [done, setDone] = useState(false);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const longEnough = newPassword.length >= 8;
  const matches = confirm.length > 0 && newPassword === confirm;

  async function onSubmit(e) {
    e.preventDefault();
    setError('');
    if (!longEnough) return setError('Password must be at least 8 characters.');
    if (newPassword !== confirm) return setError('Passwords do not match.');
    setBusy(true);
    try {
      await api.resetPassword(token, newPassword);
      setDone(true);
    } catch (err) {
      setError(errMsg(err, 'Reset failed'));
    } finally {
      setBusy(false);
    }
  }

  return (
    <AuthLayout>
      <div className="auth-card card pad">
        <h2 style={{ marginBottom: '.25rem' }}>Reset password</h2>
        {!token ? (
          <div className="alert error"><AlertCircle size={18} />This reset link is missing its token. Please use the link from your email.</div>
        ) : done ? (
          <>
            <div className="alert ok"><CheckCircle2 size={18} />Your password has been reset. You can now log in.</div>
            <Link className="btn green block" to="/login">Go to login</Link>
          </>
        ) : (
          <>
            <p className="muted" style={{ marginTop: 0 }}>Choose a new password for your account.</p>
            {error && <div className="alert error"><AlertCircle size={18} />{error}</div>}
            <form onSubmit={onSubmit}>
              <div className="field">
                <label>New password</label>
                <PasswordInput value={newPassword} onChange={(e) => setNew(e.target.value)} placeholder="At least 8 characters" autoFocus invalid={newPassword.length > 0 && !longEnough} />
              </div>
              <div className="field">
                <label>Confirm password</label>
                <PasswordInput value={confirm} onChange={(e) => setConfirm(e.target.value)} placeholder="Re-enter new password" invalid={confirm.length > 0 && !matches} />
                {confirm.length > 0 && !matches && <div className="err"><AlertCircle size={13} /> Passwords do not match</div>}
              </div>
              <button className="btn green block lg" disabled={busy || !longEnough || !matches}>
                {busy ? <span className="spinner" /> : 'Reset password'}
              </button>
            </form>
          </>
        )}
      </div>
    </AuthLayout>
  );
}
