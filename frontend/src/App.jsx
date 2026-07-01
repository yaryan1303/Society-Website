import { lazy, Suspense } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth/AuthContext';

const Landing = lazy(() => import('./pages/Landing'));
const Login = lazy(() => import('./pages/Login'));
const ForgotPassword = lazy(() => import('./pages/ForgotPassword'));
const ResetPassword = lazy(() => import('./pages/ResetPassword'));
const FirstLogin = lazy(() => import('./pages/FirstLogin'));
const AdminDashboard = lazy(() => import('./pages/admin/AdminDashboard'));
const MemberLedger = lazy(() => import('./pages/admin/MemberLedger'));
const MemberDashboard = lazy(() => import('./pages/member/MemberDashboard'));

/** Route guard: requires auth, enforces forced password change, checks role. */
function Protected({ role, children }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (user.mustChangePassword) return <Navigate to="/first-login" replace />;
  if (role && user.role !== role) {
    return <Navigate to={user.role === 'ADMIN' ? '/admin' : '/me'} replace />;
  }
  return children;
}

function PageLoader() {
  return <div className="page-loading"><span className="spinner" style={{ color: 'var(--primary)' }} /></div>;
}

export default function App() {
  return (
    <Suspense fallback={<PageLoader />}>
      <Routes>
        <Route path="/" element={<Landing />} />
        <Route path="/login" element={<Login />} />
        <Route path="/forgot" element={<ForgotPassword />} />
        <Route path="/reset" element={<ResetPassword />} />
        <Route path="/first-login" element={<FirstLogin />} />

        <Route path="/admin" element={<Protected role="ADMIN"><AdminDashboard /></Protected>} />
        <Route path="/admin/members/:id" element={<Protected role="ADMIN"><MemberLedger /></Protected>} />

        <Route path="/me" element={<Protected role="MEMBER"><MemberDashboard /></Protected>} />

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Suspense>
  );
}
