import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Download, Plus, Trash2, Layers, PiggyBank, Wallet, HandCoins, Receipt, AlertCircle, Lock } from 'lucide-react';
import AppLayout from '../../components/AppLayout';
import LedgerTable from '../../components/LedgerTable';
import LoanSection from './LoanSection';
import { EmptyState, SkeletonRows } from '../../components/ui';
import { useToast } from '../../components/ui/Toast';
import { useConfirm } from '../../components/ui/ConfirmDialog';
import { api, downloadBlob } from '../../api';
import { errMsg } from '../../api/client';
import { money, fmtDate, todayIso } from '../../utils/format';

const LEDGER_TABS = [
  { key: 'shares', label: 'Shares', icon: Layers },
  { key: 'compulsory-deposits', label: 'Compulsory Deposit', icon: PiggyBank },
  { key: 'other-deposits', label: 'Other Deposit', icon: Wallet },
];

export default function MemberLedger() {
  const { id } = useParams();
  const toast = useToast();
  const [member, setMember] = useState(null);
  const [years, setYears] = useState([]);
  const [yearId, setYearId] = useState('');
  const [tab, setTab] = useState('shares');
  const [ledgers, setLedgers] = useState({});
  const [loans, setLoans] = useState([]);
  const [favour, setFavour] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const selectedYear = years.find((y) => String(y.id) === String(yearId));
  const yearClosed = selectedYear?.closed;

  useEffect(() => {
    Promise.all([api.getMember(id), api.listYears()])
      .then(([m, y]) => {
        setMember(m.data);
        setYears(y.data);
        const open = y.data.find((yr) => !yr.closed) || y.data[0];
        if (open) setYearId(String(open.id));
      })
      .catch((err) => setError(errMsg(err)));
  }, [id]);

  const loadYearData = useCallback(async () => {
    if (!yearId) return;
    setLoading(true);
    try {
      const [sh, cd, od, ln, fv] = await Promise.all([
        api.ledger(id, 'shares', yearId), api.ledger(id, 'compulsory-deposits', yearId),
        api.ledger(id, 'other-deposits', yearId), api.listLoans(id), api.favourStood(id, yearId),
      ]);
      setLedgers({ shares: sh.data, 'compulsory-deposits': cd.data, 'other-deposits': od.data });
      setLoans(ln.data);
      setFavour(fv.data);
    } catch (err) { toast.error('Could not load ledger', errMsg(err)); }
    finally { setLoading(false); }
  }, [id, yearId]);

  useEffect(() => { loadYearData(); }, [loadYearData]);

  async function onExport() {
    try {
      const res = await api.exportMember(id, yearId || undefined);
      downloadBlob(res, `ledger_${member?.accountNo}.xlsx`);
      toast.success('Export ready', 'Ledger downloaded.');
    } catch (err) { toast.error('Export failed', errMsg(err)); }
  }

  if (error && !member) return <AppLayout title="Member"><div className="alert error"><AlertCircle size={18} />{error}</div></AppLayout>;
  if (!member) return <AppLayout title="Member ledger"><SkeletonRows rows={6} /></AppLayout>;

  const actions = (
    <>
      <select className="input" style={{ width: 'auto' }} value={yearId} onChange={(e) => setYearId(e.target.value)}>
        {years.map((y) => <option key={y.id} value={y.id}>{y.label}{y.closed ? ' (closed)' : ''}</option>)}
      </select>
      <button className="btn ghost sm" onClick={onExport}><Download size={15} /> Export</button>
    </>
  );

  return (
    <AppLayout
      title={member.name}
      breadcrumb={<><Link to="/admin">Members</Link> / <b>{member.name}</b></>}
      actions={actions}
    >
      <p className="muted" style={{ marginTop: '-.75rem', marginBottom: '1.1rem' }}>
        A/c {member.accountNo}{member.fatherOrHusbandName ? ` · ${member.fatherOrHusbandName}` : ''} · {member.email}
      </p>

      {yearClosed && <div className="alert info"><Lock size={18} />This year is closed — entries are locked. Switch to an open year to make changes.</div>}

      <div className="tabs">
        {LEDGER_TABS.map((t) => (
          <button key={t.key} className={`tab ${tab === t.key ? 'active' : ''}`} onClick={() => setTab(t.key)}><t.icon /> {t.label}</button>
        ))}
        <button className={`tab ${tab === 'loans' ? 'active' : ''}`} onClick={() => setTab('loans')}><HandCoins /> Loans & Interest</button>
        <button className={`tab ${tab === 'favour' ? 'active' : ''}`} onClick={() => setTab('favour')}><Receipt /> Favour Stood</button>
      </div>

      {loading ? <SkeletonRows rows={5} />
        : tab === 'loans' ? <LoanSection memberId={id} yearId={yearId} loans={loans} yearClosed={yearClosed} onChange={loadYearData} />
        : tab === 'favour' ? <FavourSection memberId={id} yearId={yearId} entries={favour} yearClosed={yearClosed} onChange={loadYearData} />
        : <LedgerSection memberId={id} section={tab} yearId={yearId} yearClosed={yearClosed} view={ledgers[tab]}
                         defaultCr={tab === 'compulsory-deposits' ? member.compulsoryDepositAmount : ''} onChange={loadYearData} />}
    </AppLayout>
  );
}

function LedgerSection({ memberId, section, yearId, view, defaultCr, yearClosed, onChange }) {
  const toast = useToast();
  const confirm = useConfirm();
  const [form, setForm] = useState({ txnDate: todayIso(), dr: '', cr: '', particulars: '' });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  useEffect(() => {
    if (section === 'compulsory-deposits' && defaultCr) setForm((f) => ({ ...f, cr: f.cr || String(defaultCr) }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [section, defaultCr]);

  async function add(e) {
    e.preventDefault();
    setError('');
    setBusy(true);
    try {
      await api.addLedger(memberId, section, {
        txnDate: form.txnDate, dr: form.dr ? Number(form.dr) : 0, cr: form.cr ? Number(form.cr) : 0,
        particulars: form.particulars || null,
      }, yearId);
      setForm({ txnDate: todayIso(), dr: '', cr: section === 'compulsory-deposits' && defaultCr ? String(defaultCr) : '', particulars: '' });
      toast.success('Entry recorded');
      onChange();
    } catch (err) { setError(errMsg(err)); }
    finally { setBusy(false); }
  }

  async function onDelete(entry) {
    const ok = await confirm({ title: 'Delete this entry?', message: 'Balances will be recomputed for the year.', confirmText: 'Delete' });
    if (!ok) return;
    try { await api.deleteLedger(memberId, section, entry.id); toast.success('Entry deleted'); onChange(); }
    catch (err) { toast.error('Delete failed', errMsg(err)); }
  }

  return (
    <div className="stack">
      {!yearClosed && (
        <form className="panel" onSubmit={add}>
          <strong>Record an entry</strong>
          {error && <div className="alert error" style={{ marginTop: '.5rem' }}><AlertCircle size={18} />{error}</div>}
          <div className="row" style={{ marginTop: '.75rem', alignItems: 'flex-end' }}>
            <div className="field" style={{ margin: 0 }}><label>Date</label><input type="date" className="input" value={form.txnDate} onChange={set('txnDate')} required /></div>
            <div className="field" style={{ margin: 0 }}><label>Dr (reduction)</label><input type="number" step="0.01" min="0" className="input" value={form.dr} onChange={set('dr')} placeholder="0.00" /></div>
            <div className="field" style={{ margin: 0 }}><label>Cr (deposit/purchase)</label><input type="number" step="0.01" min="0" className="input" value={form.cr} onChange={set('cr')} placeholder="0.00" /></div>
            <div className="field" style={{ margin: 0, flex: 1, minWidth: 160 }}><label>Particulars</label><input className="input" value={form.particulars} onChange={set('particulars')} placeholder="e.g. R.No 12499" /></div>
            <button className="btn green" disabled={busy}>{busy ? <span className="spinner" /> : <><Plus size={16} /> Add</>}</button>
          </div>
        </form>
      )}
      <LedgerTable entries={view?.entries || []} editable={!yearClosed} onDelete={onDelete} compulsory={section === 'compulsory-deposits'} />
    </div>
  );
}

function FavourSection({ memberId, yearId, entries, yearClosed, onChange }) {
  const toast = useToast();
  const confirm = useConfirm();
  const [form, setForm] = useState({ entryDate: todayIso(), amount: '', note: '' });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });
  const total = entries.reduce((s, e) => s + (Number(e.amount) || 0), 0);

  async function add(e) {
    e.preventDefault();
    setError('');
    setBusy(true);
    try {
      await api.addFavourStood(memberId, { entryDate: form.entryDate, amount: Number(form.amount), note: form.note || null }, yearId);
      setForm({ entryDate: todayIso(), amount: '', note: '' });
      toast.success('Favour-stood entry added');
      onChange();
    } catch (err) { setError(errMsg(err)); } finally { setBusy(false); }
  }

  async function onDelete(en) {
    const ok = await confirm({ title: 'Delete this favour-stood entry?', confirmText: 'Delete' });
    if (!ok) return;
    try { await api.deleteFavourStood(memberId, en.id); toast.success('Entry deleted'); onChange(); }
    catch (err) { toast.error('Delete failed', errMsg(err)); }
  }

  return (
    <div className="stack">
      <p className="muted" style={{ marginTop: 0 }}>
        Records amounts credited in members' favour. The year-end compulsory-deposit split lands here automatically, and the running total carries forward.
      </p>
      {!yearClosed && (
        <form className="panel" onSubmit={add}>
          <strong>Add entry</strong>
          {error && <div className="alert error" style={{ marginTop: '.5rem' }}><AlertCircle size={18} />{error}</div>}
          <div className="row" style={{ marginTop: '.75rem', alignItems: 'flex-end' }}>
            <div className="field" style={{ margin: 0 }}><label>Date</label><input type="date" className="input" value={form.entryDate} onChange={set('entryDate')} required /></div>
            <div className="field" style={{ margin: 0 }}><label>Amount</label><input type="number" step="0.01" min="0.01" className="input" value={form.amount} onChange={set('amount')} required /></div>
            <div className="field" style={{ margin: 0, flex: 1, minWidth: 160 }}><label>Note</label><input className="input" value={form.note} onChange={set('note')} /></div>
            <button className="btn green" disabled={busy}>{busy ? <span className="spinner" /> : <><Plus size={16} /> Add</>}</button>
          </div>
        </form>
      )}
      <div className="table-wrap">
        <table className="ledger">
          <thead><tr><th>Date</th><th className="num">Amount</th><th>Note</th>{!yearClosed && <th></th>}</tr></thead>
          <tbody>
            {entries.length === 0 && <tr><td colSpan={yearClosed ? 3 : 4}><EmptyState icon={Receipt} title="No entries this year" /></td></tr>}
            {entries.map((en) => (
              <tr key={en.id} className={en.opening ? 'opening' : ''}>
                <td>{fmtDate(en.entryDate)}</td>
                <td className="num">{money(en.amount)}</td>
                <td>{en.note}</td>
                {!yearClosed && <td className="num">{!en.opening && <button className="btn ghost sm icon danger" onClick={() => onDelete(en)}><Trash2 size={15} /></button>}</td>}
              </tr>
            ))}
          </tbody>
          {entries.length > 0 && <tfoot><tr className="total"><td>Total</td><td className="num">{money(total)}</td><td colSpan={yearClosed ? 1 : 2}></td></tr></tfoot>}
        </table>
      </div>
    </div>
  );
}
