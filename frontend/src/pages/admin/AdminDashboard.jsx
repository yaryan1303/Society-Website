import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Users, UserCheck, CalendarRange, Activity, UserPlus, Download, CalendarClock,
  Plus, KeyRound, Trash2, Search, FileText, ShieldCheck, AlertCircle,
} from 'lucide-react';
import AppLayout from '../../components/AppLayout';
import Modal from '../../components/Modal';
import { DonutChart } from '../../components/charts';
import { StatCard, EmptyState, Tooltip, SkeletonRows } from '../../components/ui';
import { useToast } from '../../components/ui/Toast';
import { useConfirm } from '../../components/ui/ConfirmDialog';
import { api, downloadBlob } from '../../api';
import { errMsg } from '../../api/client';
import { money } from '../../utils/format';

export default function AdminDashboard() {
  const navigate = useNavigate();
  const toast = useToast();
  const confirm = useConfirm();
  const [members, setMembers] = useState([]);
  const [years, setYears] = useState([]);
  const [yearId, setYearId] = useState('');
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState('');
  const [showCreate, setShowCreate] = useState(false);
  const [showClose, setShowClose] = useState(false);
  const [createdTemp, setCreatedTemp] = useState(null);

  async function load() {
    setLoading(true);
    try {
      const [m, y] = await Promise.all([api.listMembers(), api.listYears()]);
      setMembers(m.data);
      setYears(y.data);
      if (!yearId && y.data.length) {
        const open = y.data.find((yr) => !yr.closed) || y.data[0];
        setYearId(String(open.id));
      }
    } catch (err) {
      toast.error('Could not load dashboard', errMsg(err));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, []);

  async function onDelete(m) {
    const ok = await confirm({
      title: `Delete ${m.name}?`,
      message: `This permanently removes A/c ${m.accountNo} and all their ledger, loan and favour-stood data. This cannot be undone.`,
      confirmText: 'Delete member',
    });
    if (!ok) return;
    try { await api.deleteMember(m.id); toast.success('Member deleted', `${m.name} was removed.`); load(); }
    catch (err) { toast.error('Delete failed', errMsg(err)); }
  }

  async function onResetPwd(m) {
    const ok = await confirm({
      title: `Reset password for ${m.name}?`,
      message: 'A new temporary password will be generated and emailed. The member must change it on next login.',
      confirmText: 'Reset password', danger: false,
    });
    if (!ok) return;
    try {
      const { data } = await api.resetMemberPassword(m.id);
      setCreatedTemp({ name: m.name, accountNo: m.accountNo, temporaryPassword: data.temporaryPassword });
    } catch (err) { toast.error('Reset failed', errMsg(err)); }
  }

  async function onExportAll() {
    try {
      const res = await api.exportAll(yearId || undefined);
      downloadBlob(res, 'all_members.xlsx');
      toast.success('Export ready', 'All member ledgers downloaded.');
    } catch (err) { toast.error('Export failed', errMsg(err)); }
  }

  async function onAddYear() {
    const input = window.prompt('Start year of the new financial year (e.g. 2027 for 2027-28):');
    if (!input) return;
    try { await api.createYear(Number(input)); toast.success('Financial year created'); load(); }
    catch (err) { toast.error('Could not create year', errMsg(err)); }
  }

  const selectedYear = years.find((y) => String(y.id) === String(yearId));
  const activeCount = members.filter((m) => m.active).length;
  const statusData = [
    { name: 'Active', value: activeCount },
    { name: 'Inactive', value: members.length - activeCount },
  ];
  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return members;
    return members.filter((m) => `${m.name} ${m.accountNo} ${m.email}`.toLowerCase().includes(q));
  }, [members, query]);

  const actions = (
    <>
      <select className="input hide-sm" style={{ width: 'auto' }} value={yearId} onChange={(e) => setYearId(e.target.value)}>
        {years.map((y) => <option key={y.id} value={y.id}>{y.label}{y.closed ? ' (closed)' : ''}</option>)}
      </select>
      <Tooltip label="Add financial year"><button className="icon-btn" onClick={onAddYear}><CalendarClock size={17} /></button></Tooltip>
      <button className="btn ghost sm hide-sm" onClick={onExportAll}><Download size={15} /> Export all</button>
      <button className="btn ghost sm hide-sm" onClick={() => setShowClose(true)} disabled={!selectedYear || selectedYear.closed}><CalendarRange size={15} /> Year-end</button>
      <button className="btn green sm" onClick={() => setShowCreate(true)}><UserPlus size={15} /> New Member</button>
    </>
  );

  return (
    <AppLayout title="Admin Dashboard" actions={actions}>
      <p className="muted" style={{ marginTop: '-.75rem', marginBottom: '1.25rem' }}>Manage members, ledgers, year-end close and exports.</p>

      {loading ? <SkeletonRows rows={6} /> : (
        <>
          <div className="stats" style={{ marginBottom: '1.25rem' }}>
            <StatCard icon={Users} label="Total Members" value={members.length} />
            <StatCard icon={UserCheck} tone="teal" label="Active" value={activeCount} sub={`${members.length - activeCount} inactive`} />
            <StatCard icon={CalendarRange} tone="amber" label="Financial Years" value={years.length} />
            <StatCard icon={Activity} tone={selectedYear?.closed ? 'red' : 'teal'} label="Selected Year" value={selectedYear?.label || '—'} sub={selectedYear ? (selectedYear.closed ? 'Closed' : 'Open') : ''} />
          </div>

          <div className="grid-2" style={{ marginBottom: '1.5rem' }}>
            <div className="card pad">
              <div className="card-head"><h3>Members by status</h3></div>
              {members.length
                ? <DonutChart data={statusData} colors={['#0f766e', '#94a3b8']} center={{ label: 'Members', value: members.length }} />
                : <EmptyState icon={Users} title="No members yet" />}
            </div>
            <div className="card pad">
              <div className="card-head"><h3>Quick actions</h3></div>
              <div className="stack">
                <button className="btn ghost block" onClick={() => setShowCreate(true)}><UserPlus size={16} /> Add a new member</button>
                <button className="btn ghost block" onClick={onExportAll}><Download size={16} /> Export all ledgers ({selectedYear?.label || '—'})</button>
                <button className="btn ghost block" onClick={() => setShowClose(true)} disabled={!selectedYear || selectedYear.closed}>
                  <CalendarRange size={16} /> Run year-end close
                </button>
              </div>
            </div>
          </div>

          <div className="card">
            <div className="card-head" style={{ padding: '1.1rem 1.25rem 0' }}>
              <h3>Members <span className="badge neutral nodot">{filtered.length}</span></h3>
              <div className="input-group" style={{ maxWidth: 280 }}>
                <span className="lead-icon"><Search /></span>
                <input className="input" placeholder="Search name, A/c or email…" value={query} onChange={(e) => setQuery(e.target.value)} />
              </div>
            </div>
            <div className="table-wrap" style={{ border: 'none', boxShadow: 'none', marginTop: '.75rem' }}>
              <table className="ledger">
                <thead>
                  <tr><th>Member</th><th>Email</th><th className="num">Monthly Deposit</th><th>Status</th><th></th></tr>
                </thead>
                <tbody>
                  {filtered.length === 0 && <tr><td colSpan={5}><EmptyState icon={Users} title="No members found" message={query ? 'Try a different search.' : 'Create your first member.'} /></td></tr>}
                  {filtered.map((m) => (
                    <tr key={m.id}>
                      <td>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '.65rem' }}>
                          <span className="avatar" style={{ flex: 'none' }}>{m.name.split(/\s+/).slice(0, 2).map((w) => w[0]).join('').toUpperCase()}</span>
                          <div><div style={{ fontWeight: 600 }}>{m.name}</div><div className="muted" style={{ fontSize: '.78rem' }}>A/c {m.accountNo}</div></div>
                        </div>
                      </td>
                      <td className="muted">{m.email}</td>
                      <td className="num">{money(m.compulsoryDepositAmount)}</td>
                      <td>{m.active ? <span className="badge green">Active</span> : <span className="badge gray">Inactive</span>}</td>
                      <td className="num">
                        <div style={{ display: 'inline-flex', gap: '.4rem' }}>
                          <button className="btn sm" onClick={() => navigate(`/admin/members/${m.id}`)}><FileText size={14} /> Ledger</button>
                          <Tooltip label="Reset password"><button className="btn ghost sm icon" onClick={() => onResetPwd(m)}><KeyRound size={15} /></button></Tooltip>
                          <Tooltip label="Delete member"><button className="btn ghost sm icon danger" onClick={() => onDelete(m)}><Trash2 size={15} /></button></Tooltip>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}

      {showCreate && <CreateMemberModal onClose={() => setShowCreate(false)} onCreated={(p) => { setShowCreate(false); setCreatedTemp(p); load(); }} />}

      {createdTemp && (
        <Modal title="Account credentials" size="sm" onClose={() => setCreatedTemp(null)}
               footer={<button className="btn" onClick={() => setCreatedTemp(null)}>Done</button>}>
          <div className="alert ok"><ShieldCheck size={18} />Share these with the member. The temporary password works once; they must set a new one on first login.</div>
          <p><strong>Name:</strong> {createdTemp.name}</p>
          <p><strong>Account number:</strong> {createdTemp.accountNo}</p>
          <p><strong>Temporary password:</strong> <code style={{ fontSize: '1.05rem', background: 'var(--surface-3)', padding: '.15rem .45rem', borderRadius: 6 }}>{createdTemp.temporaryPassword}</code></p>
        </Modal>
      )}

      {showClose && selectedYear && <YearEndModal year={selectedYear} onClose={() => setShowClose(false)} onClosed={() => { setShowClose(false); load(); }} />}
    </AppLayout>
  );
}

function CreateMemberModal({ onClose, onCreated }) {
  const toast = useToast();
  const [f, setF] = useState({ accountNo: '', name: '', fatherOrHusbandName: '', email: '', address: '', compulsoryDepositAmount: '1500', maxCreditLimit: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  async function submit(e) {
    e.preventDefault();
    setError('');
    setBusy(true);
    try {
      const { data } = await api.createMember({
        accountNo: f.accountNo.trim(), name: f.name.trim(),
        fatherOrHusbandName: f.fatherOrHusbandName.trim() || null, email: f.email.trim(),
        address: f.address.trim() || null,
        compulsoryDepositAmount: f.compulsoryDepositAmount ? Number(f.compulsoryDepositAmount) : null,
        maxCreditLimit: f.maxCreditLimit ? Number(f.maxCreditLimit) : null,
      });
      toast.success('Member created', `${data.member.name} (A/c ${data.member.accountNo})`);
      onCreated({ name: data.member.name, accountNo: data.member.accountNo, temporaryPassword: data.temporaryPassword });
    } catch (err) {
      setError(errMsg(err, 'Could not create member'));
    } finally { setBusy(false); }
  }

  return (
    <Modal title="New Member" onClose={onClose}
           footer={<><button className="btn ghost" onClick={onClose}>Cancel</button>
                     <button className="btn green" form="cm" disabled={busy}>{busy ? <span className="spinner" /> : 'Create member'}</button></>}>
      {error && <div className="alert error"><AlertCircle size={18} />{error}</div>}
      <form id="cm" onSubmit={submit}>
        <div className="grid-2">
          <div className="field"><label>Account number *</label><input className="input" value={f.accountNo} onChange={set('accountNo')} required placeholder="e.g. 1583" /></div>
          <div className="field"><label>Full name *</label><input className="input" value={f.name} onChange={set('name')} required /></div>
        </div>
        <div className="grid-2">
          <div className="field"><label>Father / Husband name</label><input className="input" value={f.fatherOrHusbandName} onChange={set('fatherOrHusbandName')} /></div>
          <div className="field"><label>Email *</label><input type="email" className="input" value={f.email} onChange={set('email')} required /></div>
        </div>
        <div className="field"><label>Address</label><input className="input" value={f.address} onChange={set('address')} /></div>
        <div className="grid-2">
          <div className="field"><label>Monthly compulsory deposit</label><input type="number" step="0.01" className="input" value={f.compulsoryDepositAmount} onChange={set('compulsoryDepositAmount')} /></div>
          <div className="field"><label>Max credit limit</label><input type="number" step="0.01" className="input" value={f.maxCreditLimit} onChange={set('maxCreditLimit')} /></div>
        </div>
      </form>
    </Modal>
  );
}

function YearEndModal({ year, onClose, onClosed }) {
  const toast = useToast();
  const [rows, setRows] = useState([]);
  const [splits, setSplits] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api.yearEndPreview(year.id).then((res) => setRows(res.data)).catch((err) => setError(errMsg(err))).finally(() => setLoading(false));
  }, [year.id]);

  async function confirmClose() {
    setBusy(true);
    setError('');
    try {
      const payload = Object.entries(splits).filter(([, v]) => v && Number(v) > 0).map(([memberId, amount]) => ({ memberId: Number(memberId), amount: Number(amount) }));
      await api.closeYear(year.id, payload);
      toast.success('Year closed', `${year.label} locked and balances carried forward.`);
      onClosed();
    } catch (err) {
      setError(errMsg(err, 'Close failed'));
      setBusy(false);
    }
  }

  return (
    <Modal title={`Year-end close — ${year.label}`} size="lg" onClose={onClose}
           footer={<><button className="btn ghost" onClick={onClose}>Cancel</button>
                     <button className="btn danger" onClick={confirmClose} disabled={busy || loading}>{busy ? <span className="spinner" /> : 'Confirm close & carry forward'}</button></>}>
      <div className="alert warn"><AlertCircle size={18} />This locks {year.label} permanently. Compulsory-deposit balances stay frozen all year; this year's deposits fold in now. Optionally move part of <b>this year's deposits</b> to "favour stood"; the rest carries forward.</div>
      {error && <div className="alert error"><AlertCircle size={18} />{error}</div>}
      {loading ? <SkeletonRows rows={4} /> : (
        <div className="table-wrap">
          <table className="ledger">
            <thead><tr><th>Member</th><th className="num">Balance (frozen)</th><th className="num">Deposits this year</th><th className="num">Move to favour-stood</th><th className="num">→ Next opening</th></tr></thead>
            <tbody>
              {rows.map((r) => {
                const frozen = Number(r.compulsoryDepositClosing) || 0;
                const deposits = Number(r.compulsoryDepositDeposits) || 0;
                const favour = Number(splits[r.memberId]) || 0;
                const over = favour > deposits;
                return (
                  <tr key={r.memberId}>
                    <td>{r.name}<br /><span className="muted">A/c {r.accountNo}</span></td>
                    <td className="num">{money(frozen)}</td>
                    <td className="num">{money(deposits)}</td>
                    <td className="num">
                      <input type="number" step="0.01" min="0" max={deposits} className="input" style={{ width: 130, textAlign: 'right' }}
                             value={splits[r.memberId] || ''} onChange={(e) => setSplits({ ...splits, [r.memberId]: e.target.value })} placeholder="0.00" />
                      {over && <div className="err" style={{ justifyContent: 'flex-end' }}>exceeds deposits</div>}
                    </td>
                    <td className="num">{money(frozen + deposits - favour)}</td>
                  </tr>
                );
              })}
              {rows.length === 0 && <tr><td colSpan={5}><EmptyState icon={Users} title="No members to process" /></td></tr>}
            </tbody>
          </table>
        </div>
      )}
    </Modal>
  );
}
