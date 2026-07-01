import { useEffect, useRef, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Users, LayoutDashboard, Menu, ChevronDown, KeyRound, LogOut } from 'lucide-react';
import { useAuth } from '../auth/AuthContext';
import { SOCIETY } from './branding';
import ThemeToggle from './ThemeToggle';

const NAV = {
  ADMIN: [{ to: '/admin', label: 'Members', icon: Users }],
  MEMBER: [{ to: '/me', label: 'My Account', icon: LayoutDashboard }],
};

function initials(name = '') {
  return name.trim().split(/\s+/).slice(0, 2).map((w) => w[0]).join('').toUpperCase() || 'U';
}

/**
 * Signed-in app shell: a sticky sidebar (drawer on mobile) + topbar with
 * breadcrumb, page actions, theme toggle and a user menu.
 */
export default function AppLayout({ title, breadcrumb, actions, children }) {
  const { user, isAdmin, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [drawer, setDrawer] = useState(false);
  const [menu, setMenu] = useState(false);
  const menuRef = useRef(null);

  const items = NAV[isAdmin ? 'ADMIN' : 'MEMBER'];
  const home = isAdmin ? '/admin' : '/me';

  useEffect(() => { setDrawer(false); }, [location.pathname]);
  useEffect(() => {
    const onClick = (e) => { if (menuRef.current && !menuRef.current.contains(e.target)) setMenu(false); };
    document.addEventListener('mousedown', onClick);
    return () => document.removeEventListener('mousedown', onClick);
  }, []);

  function handleLogout() { logout(); navigate('/login'); }

  return (
    <div className="shell">
      {drawer && <div className="scrim" onClick={() => setDrawer(false)} />}
      <aside className={`sidebar ${drawer ? 'open' : ''}`}>
        <Link to={home} className="brand">
          <span className="logo">ES</span>
          <span><div className="t1">{SOCIETY.short}</div><div className="t2">Accounts</div></span>
        </Link>
        <nav className="sidebar-nav">
          <div className="label">Menu</div>
          {items.map((it) => {
            const active = location.pathname === it.to || (it.to !== '/' && location.pathname.startsWith(it.to));
            const Icon = it.icon;
            return (
              <Link key={it.to} to={it.to} className={`nav-item ${active ? 'active' : ''}`}>
                <Icon /> {it.label}
              </Link>
            );
          })}
        </nav>
        <div className="sidebar-foot">
          <button className="nav-item" onClick={handleLogout}><LogOut /> Log out</button>
        </div>
      </aside>

      <div style={{ display: 'flex', flexDirection: 'column', minWidth: 0 }}>
        <header className="topbar">
          <button className="icon-btn menu-btn" onClick={() => setDrawer(true)} aria-label="Open menu"><Menu size={18} /></button>
          <div className="crumb hide-sm">
            {breadcrumb ? <>{breadcrumb}</> : <b>{title}</b>}
          </div>
          <div className="grow" />
          {actions}
          <ThemeToggle />
          <div className="usermenu" ref={menuRef}>
            <button className="icon-btn" style={{ width: 'auto', padding: '0 .4rem 0 .35rem', gap: '.4rem', display: 'inline-flex' }}
                    onClick={() => setMenu((m) => !m)} aria-label="Account menu">
              <span className="avatar">{initials(user?.name)}</span>
              <ChevronDown size={15} className="hide-sm" />
            </button>
            {menu && (
              <div className="menu-pop">
                <div style={{ padding: '.5rem .65rem', borderBottom: '1px solid var(--border)', marginBottom: '.3rem' }}>
                  <div style={{ fontWeight: 700, fontSize: '.9rem' }}>{user?.name}</div>
                  <div className="muted" style={{ fontSize: '.78rem' }}>A/c {user?.accountNo} · {user?.role}</div>
                </div>
                <Link to="/first-login"><KeyRound size={16} /> Change password</Link>
                <button onClick={handleLogout}><LogOut size={16} /> Log out</button>
              </div>
            )}
          </div>
        </header>
        <main className="content">
          {title && <h1 style={{ marginBottom: '1.25rem' }}>{title}</h1>}
          {children}
        </main>
      </div>
    </div>
  );
}
