import { Link, useNavigate } from 'react-router-dom';
import { LogIn, LogOut, LayoutDashboard } from 'lucide-react';
import { useAuth } from '../auth/AuthContext';
import { SOCIETY } from './branding';
import ThemeToggle from './ThemeToggle';

export default function Navbar() {
  const { user, isAdmin, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() { logout(); navigate('/login'); }
  const home = user ? (isAdmin ? '/admin' : '/me') : '/';

  return (
    <header className="navbar">
      <div className="inner">
        <Link to={home} className="brand">
          <span className="logo">ES</span>
          <span>
            <div className="t1">{SOCIETY.short}</div>
            <div className="t2 hide-sm">{SOCIETY.name}</div>
          </span>
        </Link>
        <nav className="nav-actions">
          <ThemeToggle />
          {user ? (
            <>
              <span className="who hide-sm">{user.name} · <span className="badge navy nodot">{user.role}</span></span>
              <Link className="btn ghost sm" to={home}><LayoutDashboard size={15} /> Dashboard</Link>
              <button className="btn green sm" onClick={handleLogout}><LogOut size={15} /> Log out</button>
            </>
          ) : (
            <Link className="btn green sm" to="/login"><LogIn size={15} /> Member Login</Link>
          )}
        </nav>
      </div>
    </header>
  );
}
