import { Moon, Sun } from 'lucide-react';
import { useTheme } from '../theme/ThemeContext';
import { Tooltip } from './ui';

export default function ThemeToggle() {
  const { theme, toggle } = useTheme();
  const dark = theme === 'dark';
  return (
    <Tooltip label={dark ? 'Switch to light' : 'Switch to dark'}>
      <button className="icon-btn" onClick={toggle} aria-label="Toggle theme">
        {dark ? <Sun size={18} /> : <Moon size={18} />}
      </button>
    </Tooltip>
  );
}
