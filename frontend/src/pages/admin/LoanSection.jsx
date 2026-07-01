import { useEffect, useState } from 'react';
import { Plus, Percent, Receipt, Trash2, AlertCircle, Info, Landmark } from 'lucide-react';
import Modal from '../../components/Modal';
import { EmptyState } from '../../components/ui';
import { useToast } from '../../components/ui/Toast';
import { useConfirm } from '../../components/ui/ConfirmDialog';
import { api } from '../../api';
import { errMsg } from '../../api/client';
import { money, fmtDate, todayIso } from '../../utils/format';

function TypeBadge({ type }) {
  const map = { DISBURSAL: 'navy', INTEREST_CHARGE: 'warn', REPAYMENT: 'green', OPENING: 'gray' };
  const label = { DISBURSAL: 'Disbursal', INTEREST_CHARGE: 'Interest', REPAYMENT: 'Repayment', OPENING: 'Opening' };
  return <span className={`badge ${map[type] || 'gray'} nodot`}>{label[type] || type}</span>;
}

export default function LoanSection({ memberId, yearId, loans, yearClosed, onChange }) {
  const toast = useToast();
  const confirm = useConfirm();
  const [details, setDetails] = useState({});
  const [modal, setModal] = useState(null);

  useEffect(() => {
    let cancelled = false;
    Promise.all(loans.map((l) => api.loanDetail(memberId, l.id)))
      .then((res) => { if (cancelled) return; const map = {}; res.forEach((r) => { map[r.data.loan.id] = r.data; }); setDetails(map); })
      .catch(() => {});
    return () => { cancelled = true; };
  }, [memberId, loans]);

  async function deleteTxn(loanId, txnId) {
    const ok = await confirm({ title: 'Delete this transaction?', message: 'Balances and interest will be recomputed.', confirmText: 'Delete' });
    if (!ok) return;
    try { await api.deleteLoanTxn(memberId, loanId, txnId); toast.success('Transaction deleted'); onChange(); }
    catch (err) { toast.error('Delete failed', errMsg(err)); }
  }

  async function closeLoan(loanId) {
    const ok = await confirm({ title: 'Close this loan?', message: 'Only allowed when principal and interest are fully paid.', confirmText: 'Close loan', danger: false });
    if (!ok) return;
    try { await api.closeLoan(memberId, loanId); toast.success('Loan closed'); onChange(); }
    catch (err) { toast.error('Could not close loan', errMsg(err)); }
  }

  const activeLoan = loans.find((l) => !l.closed) || null;

  return (
    <div className="stack">
      {!yearClosed && (
        <div><button className="btn green" onClick={() => setModal({ type: 'disburse' })}><Plus size={16} /> {activeLoan ? 'Add to existing loan' : 'Disburse new loan'}</button></div>
      )}

      {loans.length === 0 && <div className="card"><EmptyState icon={Landmark} title="No loans yet" message="Disburse a loan to start tracking principal and interest." /></div>}

      {loans.map((loan) => (
        <div className="panel" key={loan.id}>
          <div className="spread">
            <div>
              <strong>Loan #{loan.id}</strong> {loan.bondNo && <span className="badge navy nodot">Bond {loan.bondNo}</span>}{' '}
              {loan.closed ? <span className="badge gray">Closed</span> : <span className="badge green">Active</span>}
              <div className="muted" style={{ fontSize: '.85rem', marginTop: '.2rem' }}>
                Opened {fmtDate(loan.openingDate)}{loan.sureties ? ` · Sureties: ${loan.sureties}` : ''}{loan.conditionOfRepayment ? ` · ${loan.conditionOfRepayment}` : ''}
              </div>
            </div>
            {!yearClosed && !loan.closed && (
              <div className="row">
                <button className="btn ghost sm" onClick={() => setModal({ type: 'interest', loan })}><Percent size={14} /> Post interest</button>
                <button className="btn green sm" onClick={() => setModal({ type: 'repay', loan })}><Receipt size={14} /> Repayment</button>
                <button className="btn ghost sm" onClick={() => closeLoan(loan.id)}>Close loan</button>
              </div>
            )}
          </div>

          <div className="stats" style={{ margin: '1rem 0' }}>
            <div className="stat"><div className="k">Principal Outstanding</div><div className="v">{money(loan.principalOutstanding)}</div></div>
            <div className="stat"><div className="k">Interest Outstanding</div><div className="v">{money(loan.interestOutstanding)}</div></div>
            <div className="stat"><div className="k">Interest / month @ {loan.annualRatePct}%</div><div className="v">{money(loan.projectedMonthlyInterest)}</div></div>
          </div>

          <LoanTxnTable detail={details[loan.id]} editable={!yearClosed} onDelete={(t) => deleteTxn(loan.id, t.id)} />
        </div>
      ))}

      {modal?.type === 'disburse' && <DisburseModal memberId={memberId} yearId={yearId} activeLoan={activeLoan} onClose={() => setModal(null)} onDone={() => { setModal(null); onChange(); }} />}
      {modal?.type === 'interest' && <PostInterestModal memberId={memberId} yearId={yearId} loan={modal.loan} onClose={() => setModal(null)} onDone={() => { setModal(null); onChange(); }} />}
      {modal?.type === 'repay' && <RepaymentModal memberId={memberId} yearId={yearId} loan={modal.loan} txns={details[modal.loan.id]?.txns || []} onClose={() => setModal(null)} onDone={() => { setModal(null); onChange(); }} />}
    </div>
  );
}

function LoanTxnTable({ detail, editable, onDelete }) {
  const txns = detail?.txns || [];
  return (
    <div className="table-wrap">
      <table className="ledger">
        <thead>
          <tr>
            <th>Date</th><th>Type</th>
            <th className="num">Loan Dr</th><th className="num">Loan Cr</th><th className="num">Loan Bal.</th>
            <th className="num">Int. Chgd</th><th className="num">Int. Paid</th><th className="num">Int. Bal.</th>
            <th>Mode</th><th>Receipt</th>{editable && <th></th>}
          </tr>
        </thead>
        <tbody>
          {txns.length === 0 && <tr><td colSpan={editable ? 11 : 10} className="empty">No transactions.</td></tr>}
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
              {editable && <td className="num">{t.txnType !== 'OPENING' && <button className="btn ghost sm icon danger" onClick={() => onDelete(t)}><Trash2 size={15} /></button>}</td>}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function DisburseModal({ memberId, yearId, activeLoan, onClose, onDone }) {
  const toast = useToast();
  const [f, setF] = useState({ openingDate: todayIso(), amount: '', bondNo: '', conditionOfRepayment: '', sureties: '', cbFolio: '' });
  const [error, setError] = useState(''); const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  const topUp = !!activeLoan;

  async function submit(e) {
    e.preventDefault(); setError(''); setBusy(true);
    try {
      await api.disburseLoan(memberId, {
        openingDate: f.openingDate, amount: Number(f.amount), bondNo: f.bondNo || null,
        conditionOfRepayment: f.conditionOfRepayment || null, sureties: f.sureties || null, cbFolio: f.cbFolio || null,
      }, yearId);
      toast.success(topUp ? 'Added to loan' : 'Loan disbursed');
      onDone();
    } catch (err) { setError(errMsg(err)); setBusy(false); }
  }

  return (
    <Modal title={topUp ? `Add to Loan #${activeLoan.id}` : 'Disburse new loan'} onClose={onClose}
           footer={<><button className="btn ghost" onClick={onClose}>Cancel</button>
                     <button className="btn green" form="dl" disabled={busy}>{busy ? <span className="spinner" /> : (topUp ? 'Add to loan' : 'Disburse')}</button></>}>
      {error && <div className="alert error"><AlertCircle size={18} />{error}</div>}
      {topUp && <div className="alert info"><Info size={18} />This member already has an active loan (outstanding {money(activeLoan.principalOutstanding)}). This amount is added to it — no separate loan is created.</div>}
      <form id="dl" onSubmit={submit}>
        <div className="grid-2">
          <div className="field"><label>Date *</label><input type="date" className="input" value={f.openingDate} onChange={set('openingDate')} required /></div>
          <div className="field"><label>Amount *</label><input type="number" step="0.01" min="0.01" className="input" value={f.amount} onChange={set('amount')} required /></div>
        </div>
        <div className="grid-2">
          {!topUp && <div className="field"><label>Bond No.</label><input className="input" value={f.bondNo} onChange={set('bondNo')} /></div>}
          <div className="field"><label>C.B. Folio</label><input className="input" value={f.cbFolio} onChange={set('cbFolio')} /></div>
        </div>
        {!topUp && (
          <>
            <div className="field"><label>Condition of repayment</label><input className="input" value={f.conditionOfRepayment} onChange={set('conditionOfRepayment')} /></div>
            <div className="field"><label>Sureties</label><input className="input" value={f.sureties} onChange={set('sureties')} placeholder="Guarantor name(s)" /></div>
          </>
        )}
      </form>
    </Modal>
  );
}

function PostInterestModal({ memberId, yearId, loan, onClose, onDone }) {
  const toast = useToast();
  const [txnDate, setDate] = useState(todayIso());
  const [cbFolio, setCb] = useState('');
  const [error, setError] = useState(''); const [busy, setBusy] = useState(false);

  async function submit(e) {
    e.preventDefault(); setError(''); setBusy(true);
    try { await api.postInterest(memberId, loan.id, { txnDate, cbFolio: cbFolio || null }, yearId); toast.success('Interest posted'); onDone(); }
    catch (err) { setError(errMsg(err)); setBusy(false); }
  }

  return (
    <Modal title={`Post interest — Loan #${loan.id}`} size="sm" onClose={onClose}
           footer={<><button className="btn ghost" onClick={onClose}>Cancel</button>
                     <button className="btn green" form="pi" disabled={busy}>{busy ? <span className="spinner" /> : 'Post interest'}</button></>}>
      {error && <div className="alert error"><AlertCircle size={18} />{error}</div>}
      <div className="alert info"><Info size={18} />Charges <strong>{money(loan.projectedMonthlyInterest)}</strong>/month on the current outstanding of {money(loan.principalOutstanding)}. Any unpaid months up to the date below are each charged as their own row (interest starts the month after disbursal).</div>
      <form id="pi" onSubmit={submit}>
        <div className="field"><label>Date *</label><input type="date" className="input" value={txnDate} onChange={(e) => setDate(e.target.value)} required /></div>
        <div className="field"><label>C.B. Folio</label><input className="input" value={cbFolio} onChange={(e) => setCb(e.target.value)} /></div>
      </form>
    </Modal>
  );
}

function RepaymentModal({ memberId, yearId, loan, txns = [], onClose, onDone }) {
  const toast = useToast();
  const [f, setF] = useState({ txnDate: todayIso(), amount: '', paymentMode: 'BANK', receiptNo: '', cbFolio: '' });
  const [error, setError] = useState(''); const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  const cashNoReceipt = f.paymentMode === 'CASH' && !f.receiptNo.trim();

  const amount = Number(f.amount) || 0;
  const monthly = Number(loan.projectedMonthlyInterest) || 0;
  const postedInterest = Number(loan.interestOutstanding) || 0;
  const principalDue = Number(loan.principalOutstanding) || 0;
  const ymOf = (iso) => { const p = (iso || '').split('-'); return p.length >= 2 ? Number(p[0]) * 12 + Number(p[1]) : null; };
  const lastInterest = [...txns].reverse().find((t) => t.txnType === 'INTEREST_CHARGE');
  const baselineYm = ymOf(lastInterest ? lastInterest.txnDate : loan.openingDate);
  const payYm = ymOf(f.txnDate);
  const monthsDue = baselineYm != null && payYm != null ? Math.max(0, payYm - baselineYm) : 0;
  const interestDue = postedInterest + monthsDue * monthly;
  const totalOwed = interestDue + principalDue;
  const payInterest = Math.min(amount, interestDue);
  const payPrincipal = Math.max(0, amount - payInterest);
  const overpay = amount > totalOwed + 0.005;

  async function submit(e) {
    e.preventDefault(); setError('');
    if (cashNoReceipt) return setError('Receipt number is required for CASH payments.');
    if (amount <= 0) return setError('Enter the amount paid.');
    if (overpay) return setError(`Amount exceeds total outstanding (${money(totalOwed)}).`);
    setBusy(true);
    try {
      await api.repay(memberId, loan.id, { txnDate: f.txnDate, amount, paymentMode: f.paymentMode, receiptNo: f.receiptNo.trim() || null, cbFolio: f.cbFolio || null }, yearId);
      toast.success('Repayment recorded');
      onDone();
    } catch (err) { setError(errMsg(err)); setBusy(false); }
  }

  return (
    <Modal title={`Record repayment — Loan #${loan.id}`} onClose={onClose}
           footer={<><button className="btn ghost" onClick={onClose}>Cancel</button>
                     <button className="btn green" form="rp" disabled={busy}>{busy ? <span className="spinner" /> : 'Record repayment'}</button></>}>
      {error && <div className="alert error"><AlertCircle size={18} />{error}</div>}
      <div className="alert info"><Info size={18} />Outstanding principal {money(principalDue)} · interest already posted {money(postedInterest)}.
        {monthsDue > 0 && <> Saving auto-charges <strong>{monthsDue}</strong> unpaid month{monthsDue > 1 ? 's' : ''} of interest (≈{money(monthsDue * monthly)}).</>}
        {' '}Enter the total paid — interest is cleared first, the rest reduces principal.</div>
      <form id="rp" onSubmit={submit}>
        <div className="grid-2">
          <div className="field"><label>Date *</label><input type="date" className="input" value={f.txnDate} onChange={set('txnDate')} required /></div>
          <div className="field"><label>Payment mode *</label><select className="input" value={f.paymentMode} onChange={set('paymentMode')}><option value="BANK">BANK</option><option value="CASH">CASH</option></select></div>
        </div>
        <div className="grid-2">
          <div className="field"><label>Amount paid *</label><input type="number" step="0.01" min="0.01" className="input" value={f.amount} onChange={set('amount')} placeholder="0.00" required /></div>
          <div className="field"><label>C.B. Folio</label><input className="input" value={f.cbFolio} onChange={set('cbFolio')} /></div>
        </div>
        <div className="field">
          <label>Receipt No. {f.paymentMode === 'CASH' && <span style={{ color: 'var(--danger)' }}>*</span>}</label>
          <input className="input" value={f.receiptNo} onChange={set('receiptNo')} placeholder={f.paymentMode === 'CASH' ? 'Required for cash' : 'Optional'} aria-invalid={cashNoReceipt || undefined} />
          {cashNoReceipt && <div className="err"><AlertCircle size={13} /> Required for CASH payments</div>}
        </div>
        {amount > 0 && !overpay && (
          <div className="panel" style={{ marginTop: '.25rem', background: 'var(--surface-2)' }}>
            <div className="spread"><span className="muted">Interest to clear{monthsDue > 0 ? ` (incl. ${monthsDue} new month${monthsDue > 1 ? 's' : ''})` : ''}</span><strong>{money(payInterest)}</strong></div>
            <div className="spread"><span className="muted">Principal repaid</span><strong>{money(payPrincipal)}</strong></div>
            <div className="muted" style={{ fontSize: '.78rem', marginTop: '.35rem' }}>Final split is computed on the server when you save.</div>
          </div>
        )}
        {overpay && <div className="alert error" style={{ marginTop: '.25rem' }}><AlertCircle size={18} />Amount exceeds total outstanding ({money(totalOwed)}).</div>}
      </form>
    </Modal>
  );
}
