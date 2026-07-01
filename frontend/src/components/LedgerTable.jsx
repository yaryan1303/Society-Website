import { money, fmtDate } from '../utils/format';

/**
 * Renders a Dr/Cr/running-Balance ledger (shares + both deposits) in the same
 * column order as the paper book. Read-only unless `editable` + `onDelete`.
 *
 * In `compulsory` mode the Balance column is frozen at the year's opening figure
 * (the carried-forward amount): the monthly deposits accrue in the Deposit column
 * and only fold into the balance at year-end. The footer therefore also shows this
 * year's deposits and what carries forward (before any favour-stood split).
 */
export default function LedgerTable({ entries = [], editable = false, onDelete, compulsory = false }) {
  const frozen = entries.length ? Number(entries[entries.length - 1].balanceAfter) : 0;
  const deposits = compulsory
    ? entries
        .filter((e) => !e.opening)
        .reduce((s, e) => s + (Number(e.cr) || 0) - (Number(e.dr) || 0), 0)
    : 0;
  const colSpan = editable ? 6 : 5;
  const drLabel = compulsory ? 'Withdrawal' : 'Dr';
  const crLabel = compulsory ? 'Deposit' : 'Cr';

  return (
    <div className="table-wrap">
      <table className="ledger">
        <thead>
          <tr>
            <th>Date</th>
            <th>Particulars</th>
            <th className="num">{drLabel}</th>
            <th className="num">{crLabel}</th>
            <th className="num">Balance</th>
            {editable && <th></th>}
          </tr>
        </thead>
        <tbody>
          {entries.length === 0 && (
            <tr>
              <td colSpan={colSpan} className="empty">No entries yet for this year.</td>
            </tr>
          )}
          {entries.map((e) => (
            <tr key={e.id} className={e.opening ? 'opening' : ''}>
              <td>{fmtDate(e.txnDate)}</td>
              <td>{e.particulars || (e.opening ? 'Opening balance' : '')}</td>
              <td className="num">{Number(e.dr) ? money(e.dr) : ''}</td>
              <td className="num">{Number(e.cr) ? money(e.cr) : ''}</td>
              <td className="num">{money(e.balanceAfter)}</td>
              {editable && (
                <td className="num">
                  {!e.opening && (
                    <button className="btn ghost sm danger" onClick={() => onDelete(e)} title="Delete entry">
                      Delete
                    </button>
                  )}
                </td>
              )}
            </tr>
          ))}
        </tbody>
        {entries.length > 0 && (
          <tfoot>
            <tr className="total">
              <td colSpan={4}>{compulsory ? 'Balance (frozen this year)' : 'Closing balance'}</td>
              <td className="num">{money(frozen)}</td>
              {editable && <td></td>}
            </tr>
            {compulsory && (
              <>
                <tr className="total">
                  <td colSpan={4}>Deposits this year</td>
                  <td className="num">{money(deposits)}</td>
                  {editable && <td></td>}
                </tr>
                <tr className="total">
                  <td colSpan={4}>Carries forward at year-end (before favour-stood)</td>
                  <td className="num">{money(frozen + deposits)}</td>
                  {editable && <td></td>}
                </tr>
              </>
            )}
          </tfoot>
        )}
      </table>
    </div>
  );
}
