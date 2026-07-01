import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { api } from '../api';
import { TOKEN_KEY, USER_KEY } from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  });

  useEffect(() => {
    if (user) localStorage.setItem(USER_KEY, JSON.stringify(user));
    else localStorage.removeItem(USER_KEY);
  }, [user]);

  async function login(accountNo, password) {
    const { data } = await api.login(accountNo, password);
    localStorage.setItem(TOKEN_KEY, data.token);
    const u = {
      memberId: data.memberId,
      accountNo: data.accountNo,
      name: data.name,
      role: data.role,
      mustChangePassword: data.mustChangePassword,
    };
    setUser(u);
    return u;
  }

  function logout() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    setUser(null);
  }

  function markPasswordChanged() {
    setUser((u) => (u ? { ...u, mustChangePassword: false } : u));
  }

  const value = useMemo(
    () => ({
      user,
      isAuthed: !!user,
      isAdmin: user?.role === 'ADMIN',
      login,
      logout,
      markPasswordChanged,
    }),
    [user]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
