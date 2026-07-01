import { createContext, useCallback, useContext, useRef, useState } from 'react';
import { CheckCircle2, XCircle, Info, X } from 'lucide-react';

const ToastContext = createContext(null);
const ICONS = { success: CheckCircle2, error: XCircle, info: Info };

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const seq = useRef(0);

  const dismiss = useCallback((id) => setToasts((t) => t.filter((x) => x.id !== id)), []);

  const push = useCallback((type, title, msg, ms = 4000) => {
    const id = ++seq.current;
    setToasts((t) => [...t, { id, type, title, msg }]);
    if (ms) setTimeout(() => dismiss(id), ms);
    return id;
  }, [dismiss]);

  const toast = {
    success: (title, msg) => push('success', title, msg),
    error: (title, msg) => push('error', title, msg, 6000),
    info: (title, msg) => push('info', title, msg),
  };

  return (
    <ToastContext.Provider value={toast}>
      {children}
      <div className="toast-host" role="region" aria-live="polite" aria-label="Notifications">
        {toasts.map((t) => {
          const Icon = ICONS[t.type] || Info;
          return (
            <div key={t.id} className={`toast ${t.type}`} role="status">
              <Icon className="t-ico" size={18} />
              <div style={{ flex: 1 }}>
                <div className="t-title">{t.title}</div>
                {t.msg && <div className="t-msg">{t.msg}</div>}
              </div>
              <button className="x-btn" onClick={() => dismiss(t.id)} aria-label="Dismiss"><X size={16} /></button>
            </div>
          );
        })}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used within ToastProvider');
  return ctx;
}
