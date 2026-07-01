import { useState } from 'react';
import { Lock, Eye, EyeOff } from 'lucide-react';

export default function PasswordInput({ value, onChange, placeholder = 'Password', autoFocus, id, invalid }) {
  const [show, setShow] = useState(false);
  return (
    <div className="input-group">
      <span className="lead-icon"><Lock /></span>
      <input
        id={id}
        type={show ? 'text' : 'password'}
        className="input"
        value={value}
        onChange={onChange}
        placeholder={placeholder}
        autoFocus={autoFocus}
        aria-invalid={invalid || undefined}
      />
      <button type="button" className="trail-btn" onClick={() => setShow((s) => !s)} aria-label={show ? 'Hide password' : 'Show password'}>
        {show ? <EyeOff size={17} /> : <Eye size={17} />}
      </button>
    </div>
  );
}
