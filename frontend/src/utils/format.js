const inr = new Intl.NumberFormat('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

/** Format a number/string as Indian rupees (₹1,23,456.00). Blank for null. */
export function money(value) {
  if (value === null || value === undefined || value === '') return '';
  const n = typeof value === 'number' ? value : Number(value);
  if (Number.isNaN(n)) return String(value);
  return `₹${inr.format(n)}`;
}

/** dd-mm-yyyy for display. */
export function fmtDate(iso) {
  if (!iso) return '';
  const [y, m, d] = iso.split('-');
  return `${d}-${m}-${y}`;
}

/** Today as yyyy-mm-dd (for date input defaults). */
export function todayIso() {
  return new Date().toISOString().slice(0, 10);
}
