import { useCallback, useEffect, useState } from 'react';
import { PiggyBank, Layers, Wallet, Landmark, Percent, Download, FileText, HandCoins, Receipt } from 'lucide-react';
import AppLayout from '../../components/AppLayout';
import LedgerTable from '../../components/LedgerTable';
import { DonutChart, MiniBarChart, CHART_COLORS } from '../../components/charts';
import { StatCard, EmptyState, SkeletonRows } from '../../components/ui';
import { useToast } from '../../components/ui/Toast';
import { useAuth } from '../../auth/AuthContext';
import { api, downloadBlob } from '../../api';
import { errMsg } from '../../api/client';
import { money, fmtDate } from '../../utils/format';

const LEDGER_TABS = [
  { key: 'shares', label: 'Shares', icon: Layers },
  { key: 'compulsory-deposits', label: 'Compulsory Deposit', icon: PiggyBank },
  { key: 'other-deposits', label: 'Other Deposit', icon: Wallet },
];

export default function MemberDashboard() {
  const { user } = useAuth();
  const toast = useToast();
  const memberId = user.memberId;
  const [years, setYears] = useState([]);
  const [yearId, setYearId] = useState('');
  const [tab, setTab] = useState('shares');
  const [ledgers, setLedgers] = useState({});
  const [loans, setLoans] = useState([]);
  const [loanTxns, setLoanTxns] = useState({});
  const [favour, setFavour] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.listYears().then((y) => {
      setYears(y.data);
      const open = y.data.find((yr) => !yr.closed) || y.data[0];
      if (open) setYearId(String(open.id));
    }).catch((err) => toast.error('Could not load years', errMsg(err)));
  }, []);

  const load = useCallback(async () => {
    if (!yearId) return;
    setLoading(true);
    try {
      const [sh, cd, od, ln, fv] = await Promise.all([
        api.ledger(memberId, 'shares', yearId),
        api.ledger(memberId, 'compulsory-deposits', yearId),
        api.ledger(memberId, 'other-deposits', yearId),
        api.listLoans(memberId),
        api.favourStood(memberId, yearId),
      ]);
      setLedgers({ shares: sh.data, 'compulsory-deposits': cd.data, 'other-deposits': od.data });
      setLoans(ln.data);
      setFavour(fv.data);
      const details = await Promise.all(ln.data.map((l) => api.loanDetail(memberId, l.id)));
      const map = {};
      details.forEach((r) => { map[r.data.loan.id] = r.data.txns; });
      setLoanTxns(map);
    } catch (err) {
      toast.error('Could not load your account', errMsg(err));
    } finally {
      setLoading(false);
    }
  }, [memberId, yearId]);

  useEffect(() => { load(); }, [load]);

  async function onExport() {
    try {
      const res = await api.exportMember(memberId, yearId || undefined);
      downloadBlob(res, 'my_ledger.xlsx');
      toast.success('Export ready', 'Your ledger has been downloaded.');
    } catch (err) { toast.error('Export failed', errMsg(err)); }
  }

  const shares = Number(ledgers.shares?.closingBalance || 0);
  const cd = Number(ledgers['compulsory-deposits']?.closingBalance || 0);
  const od = Number(ledgers['other-deposits']?.closingBalance || 0);
  const loanOutstanding = loans.reduce((s, l) => s + Number(l.principalOutstanding), 0);
  const interestDue = loans.reduce((s, l) => s + Number(l.interestOutstanding), 0);
  const holdings = [
    { name: 'Shares', value: shares },
    { name: 'Compulsory', value: cd },
    { name: 'Other', value: od },
  ];

  const actions = (
    <>
      <select className="input" style={{ width: 'auto' }} value={yearId} onChange={(e) => setYearId(e.target.value)}>
        {years.map((y) => <option key={y.id} value={y.id}>{y.label}{y.closed ? ' (closed)' : ''}</option>)}
      </select>
      <button className="btn ghost sm" onClick={onExport}><Download size={15} /> Export</button>
    </>
  );

  return (
    <AppLayout title="My Account" actions={actions}>
      <p className="muted" style={{ marginTop: '-.75rem', marginBottom: '1.25rem' }}>{user.name} · A/c {user.accountNo}</p>

      {loading ? (
        <>
          <div className="stats" style={{ marginBottom: '1.25rem' }}>
            {Array.from({ length: 5 }).map((_, i) => <div className="stat card" key={i}><SkeletonRows rows={1} /></div>)}
          </div>
          <SkeletonRows rows={5} />
        </>
      ) : (
        <>
          <div className="stats" style={{ marginBottom: '1.25rem' }}>
            <StatCard icon={Layers} tone="" label="Share Balance" value={money(shares)} />
            <StatCard icon={PiggyBank} tone="teal" label="Compulsory Deposit" value={money(cd)} />
            <StatCard icon={Wallet} tone="teal" label="Other Deposit" value={money(od)} />
            <StatCard icon={Landmark} tone="amber" label="Loan Outstanding" value={money(loanOutstanding)} />
            <StatCard icon={Percent} tone="red" label="Interest Due" value={money(interestDue)} />
          </div>

          <div className="grid-2" style={{ marginBottom: '1.5rem' }}>
            <div className="card pad">
              <div className="card-head"><h3>Holdings</h3><span className="badge neutral nodot">{years.find((y) => String(y.id) === String(yearId))?.label}</span></div>
              {shares + cd + od > 0
                ? <DonutChart data={holdings} center={{ label: 'Total', value: money(shares + cd + od) }} />
                : <EmptyState icon={PiggyBank} title="No holdings yet" message="Shares and deposits will appear here." />}
            </div>
            <div className="card pad">
              <div className="card-head"><h3>Loan position</h3></div>
              {loanOutstanding + interestDue > 0
                ? <MiniBarChart data={[
                    { name: 'Principal', value: loanOutstanding, color: CHART_COLORS[0] },
                    { name: 'Interest', value: interestDue, color: CHART_COLORS[2] },
                  ]} />
                : <EmptyState icon={Landmark} title="No active loan" message="You have no outstanding loan." />}
            </div>
          </div>

          <div className="tabs">
            {LEDGER_TABS.map((t) => (
              <button key={t.key} className={`tab ${tab === t.key ? 'active' : ''}`} onClick={() => setTab(t.key)}><t.icon /> {t.label}</button>
            ))}
            <button className={`tab ${tab === 'loans' ? 'active' : ''}`} onClick={() => setTab('loans')}><HandCoins /> Loans & Interest</button>
            <button className={`tab ${tab === 'favour' ? 'active' : ''}`} onClick={() => setTab('favour')}><Receipt /> Favour Stood</button>
          </div>

          {tab === 'loans' ? (
            <div className="stack">
              {loans.length === 0 && <div className="card"><EmptyState icon={Landmark} title="No loans" message="You have no loans on record." /></div>}
              {loans.map((loan) => (
                <div className="panel" key={loan.id}>
                  <div className="spread">
                    <strong>Loan #{loan.id} {loan.bondNo && <span className="badge navy nodot">Bond {loan.bondNo}</span>} {loan.closed ? <span className="badge gray">Closed</span> : <span className="badge green">Active</span>}</strong>
                  </div>
                  <div className="stats" style={{ margin: '1rem 0' }}>
                    <div className="stat"><div className="k">Outstanding</div><div className="v">{money(loan.principalOutstanding)}</div></div>
                    <div className="stat"><div className="k">Interest Due</div><div className="v">{money(loan.interestOutstanding)}</div></div>
                    <div className="stat"><div className="k">Interest / month</div><div className="v">{money(loan.projectedMonthlyInterest)}</div></div>
                  </div>
                  <ReadOnlyLoanTable txns={loanTxns[loan.id] || []} />
                </div>
              ))}
            </div>
          ) : tab === 'favour' ? (
            <div className="table-wrap">
              <table className="ledger">
                <thead><tr><th>Date</th><th className="num">Amount</th><th>Note</th></tr></thead>
                <tbody>
                  {favour.length === 0 && <tr><td colSpan={3}><EmptyState icon={Receipt} title="No entries this year" /></td></tr>}
                  {favour.map((en) => (
                    <tr key={en.id} className={en.opening ? 'opening' : ''}><td>{fmtDate(en.entryDate)}</td><td className="num">{money(en.amount)}</td><td>{en.note}</td></tr>
                  ))}
                </tbody>
                {favour.length > 0 && (
                  <tfoot><tr className="total"><td>Total</td><td className="num">{money(favour.reduce((s, e) => s + (Number(e.amount) || 0), 0))}</td><td></td></tr></tfoot>
                )}
              </table>
            </div>
          ) : (
            <LedgerTable entries={ledgers[tab]?.entries || []} editable={false} compulsory={tab === 'compulsory-deposits'} />
          )}
        </>
      )}
    </AppLayout>
  );
}

function ReadOnlyLoanTable({ txns }) {
  return (
    <div className="table-wrap">
      <table className="ledger">
        <thead>
          <tr>
            <th>Date</th><th>Type</th>
            <th className="num">Loan Dr</th><th className="num">Loan Cr</th><th className="num">Loan Bal.</th>
            <th className="num">Int. Chgd</th><th className="num">Int. Paid</th><th className="num">Int. Bal.</th>
            <th>Mode</th><th>Receipt</th>
          </tr>
        </thead>
        <tbody>
          {txns.length === 0 && <tr><td colSpan={10} className="empty">No transactions.</td></tr>}
          {txns.map((t) => (
            <tr key={t.id} className={t.txnType === 'OPENING' ? 'opening' : ''}>
              <td>{fmtDate(t.txnDate)}</td><td><TypeBadge type={t.txnType} /></td>
              <td className="num">{Number(t.loanDr) ? money(t.loanDr) : ''}</td>
              <td className="num">{Number(t.loanCr) ? money(t.loanCr) : ''}</td>
              <td className="num">{money(t.loanBalanceAfter)}</td>
              <td className="num">{Number(t.interestCharged) ? money(t.interestCharged) : ''}</td>
              <td className="num">{Number(t.interestPaid) ? money(t.interestPaid) : ''}</td>
              <td className="num">{money(t.interestBalanceAfter)}</td>
              <td>{t.paymentMode || ''}</td><td>{t.receiptNo || ''}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export function TypeBadge({ type }) {
  const map = {
    DISBURSAL: 'navy', INTEREST_CHARGE: 'warn', REPAYMENT: 'green', OPENING: 'gray',
  };
  const label = { DISBURSAL: 'Disbursal', INTEREST_CHARGE: 'Interest', REPAYMENT: 'Repayment', OPENING: 'Opening' };
  return <span className={`badge ${map[type] || 'gray'} nodot`}>{label[type] || type}</span>;
}
