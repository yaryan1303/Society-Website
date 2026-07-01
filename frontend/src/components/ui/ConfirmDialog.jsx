import { createContext, useCallback, useContext, useRef, useState } from 'react';
import { AlertTriangle } from 'lucide-react';
import Modal from '../Modal';

const ConfirmContext = createContext(null);

/**
 * Promise-based confirm dialog to replace window.confirm with a styled modal.
 * Usage: const ok = await confirm({ title, message, confirmText, danger });
 */
export function ConfirmProvider({ children }) {
  const [state, setState] = useState(null);
  const resolver = useRef(null);

  const confirm = useCallback((opts) => new Promise((resolve) => {
    resolver.current = resolve;
    setState({
      title: opts.title || 'Are you sure?',
      message: opts.message || '',
      confirmText: opts.confirmText || 'Confirm',
      cancelText: opts.cancelText || 'Cancel',
      danger: opts.danger ?? true,
    });
  }), []);

  const close = (result) => {
    resolver.current?.(result);
    resolver.current = null;
    setState(null);
  };

  return (
    <ConfirmContext.Provider value={confirm}>
      {children}
      {state && (
        <Modal title={state.title} onClose={() => close(false)}
               footer={<>
                 <button className="btn ghost" onClick={() => close(false)}>{state.cancelText}</button>
                 <button className={`btn ${state.danger ? 'danger' : 'green'}`} onClick={() => close(true)} autoFocus>{state.confirmText}</button>
               </>}>
          <div style={{ display: 'flex', gap: '.85rem', alignItems: 'flex-start' }}>
            <div style={{ flex: 'none', width: 40, height: 40, borderRadius: 12, display: 'grid', placeItems: 'center',
                          background: state.danger ? 'var(--danger-soft)' : 'var(--warning-soft)',
                          color: state.danger ? 'var(--danger-fg)' : 'var(--warning-fg)' }}>
              <AlertTriangle size={20} />
            </div>
            <p style={{ margin: 0, color: 'var(--text-muted)' }}>{state.message}</p>
          </div>
        </Modal>
      )}
    </ConfirmContext.Provider>
  );
}

export function useConfirm() {
  const ctx = useContext(ConfirmContext);
  if (!ctx) throw new Error('useConfirm must be used within ConfirmProvider');
  return ctx;
}
