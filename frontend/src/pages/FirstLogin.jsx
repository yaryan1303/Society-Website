import { useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { AlertCircle, CheckCircle2, ShieldCheck } from 'lucide-react';
import AuthLayout from '../components/AuthLayout';
import PasswordInput from '../components/ui/PasswordInput';
import { useAuth } from '../auth/AuthContext';
import { useToast } from '../components/ui/Toast';
import { api } from '../api';
import { errMsg } from '../api/client';

export default function FirstLogin() {
  const { user, markPasswordChanged } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const [currentPassword, setCurrent] = useState('');
  const [newPassword, setNew] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  if (!user) return <Navigate to="/login" replace />;

  const longEnough = newPassword.length >= 8;
  const matches = confirm.length > 0 && newPassword === confirm;

  async function onSubmit(e) {
    e.preventDefault();
    setError('');
    if (!longEnough) return setError('New password must be at least 8 characters.');
    if (newPassword !== confirm) return setError('New password and confirmation do not match.');
    setBusy(true);
    try {
      await api.changePassword(currentPassword, newPassword);
      markPasswordChanged();
      toast.success('Password updated', 'Your new password is active.');
      navigate(user.role === 'ADMIN' ? '/admin' : '/me');
    } catch (err) {
      setError(errMsg(err, 'Could not change password'));
    } finally {
      setBusy(false);
    }
  }

  return (
    <AuthLayout>
      <div className="auth-card card pad">
        <div style={{ width: 44, height: 44, borderRadius: 12, background: 'var(--accent-soft)', color: 'var(--accent)', display: 'grid', placeItems: 'center', marginBottom: '.75rem' }}>
          <ShieldCheck size={22} />
        </div>
        <h2 style={{ marginBottom: '.25rem' }}>Set a new password</h2>
        <p className="muted" style={{ marginTop: 0 }}>
          {user.mustChangePassword ? 'For your security, please set a new password before continuing.' : 'Change your account password.'}
        </p>
        {error && <div className="alert error"><AlertCircle size={18} />{error}</div>}
        <form onSubmit={onSubmit}>
          <div className="field">
            <label>Current / temporary password</label>
            <PasswordInput value={currentPassword} onChange={(e) => setCurrent(e.target.value)} placeholder="Current password" autoFocus />
          </div>
          <div className="field">
            <label>New password</label>
            <PasswordInput value={newPassword} onChange={(e) => setNew(e.target.value)} placeholder="At least 8 characters" invalid={newPassword.length > 0 && !longEnough} />
            {newPassword.length > 0 && (
              <div className={longEnough ? 'hint' : 'err'} style={longEnough ? { color: 'var(--success-fg)' } : undefined}>
                {longEnough ? <><CheckCircle2 size={13} style={{ verticalAlign: '-2px' }} /> Strong enough</> : 'Must be at least 8 characters'}
              </div>
            )}
          </div>
          <div className="field">
            <label>Confirm new password</label>
            <PasswordInput value={confirm} onChange={(e) => setConfirm(e.target.value)} placeholder="Re-enter new password" invalid={confirm.length > 0 && !matches} />
            {confirm.length > 0 && !matches && <div className="err"><AlertCircle size={13} /> Passwords do not match</div>}
          </div>
          <button className="btn green block lg" disabled={busy || !longEnough || !matches || !currentPassword}>
            {busy ? <span className="spinner" /> : 'Update password'}
          </button>
        </form>
      </div>
    </AuthLayout>
  );
}
