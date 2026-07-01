package com.esicsociety.ams.ledger;

import com.esicsociety.ams.common.Money;
import com.esicsociety.ams.common.exception.ApiExceptions.ConflictException;
import com.esicsociety.ams.common.exception.ApiExceptions.NotFoundException;
import com.esicsociety.ams.financialyear.FinancialYear;
import com.esicsociety.ams.ledger.dto.LedgerDtos;
import com.esicsociety.ams.member.Member;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

/**
 * Generic engine for the three Dr/Cr/Balance ledgers (shares, compulsory deposit,
 * other deposit). The running {@code balanceAfter} is always computed here and
 * never trusted from the client. Edits/deletes recompute the whole year in
 * insertion order so balances stay consistent.
 */
@Service
public class LedgerService {

    @Transactional(readOnly = true)
    public <T extends AbstractLedgerTxn> LedgerDtos.LedgerView view(
            LedgerTxnRepository<T> repo, String section, Long memberId, Long yearId) {
        List<T> entries = repo.findByMember_IdAndFinancialYear_IdOrderByIdAsc(memberId, yearId);
        BigDecimal opening = entries.stream().filter(AbstractLedgerTxn::isOpening)
                .map(AbstractLedgerTxn::getBalanceAfter).findFirst().orElse(Money.ZERO);
        BigDecimal closing = entries.isEmpty()
                ? Money.ZERO : entries.get(entries.size() - 1).getBalanceAfter();
        return new LedgerDtos.LedgerView(section, memberId, yearId, opening, closing,
                entries.stream().map(LedgerDtos.EntryResponse::of).toList());
    }

    /** Current closing balance for a member-year (0 if no entries). */
    @Transactional(readOnly = true)
    public <T extends AbstractLedgerTxn> BigDecimal closingBalance(
            LedgerTxnRepository<T> repo, Long memberId, Long yearId) {
        List<T> entries = repo.findByMember_IdAndFinancialYear_IdOrderByIdAsc(memberId, yearId);
        return entries.isEmpty() ? Money.ZERO : entries.get(entries.size() - 1).getBalanceAfter();
    }

    /**
     * Net deposits recorded during a member-year (sum of Cr minus Dr over the
     * non-opening rows). For compulsory deposit this is the amount that folds
     * into the frozen balance at year-end; the figure is meaningless for the
     * running-balance ledgers and is used only by the year-end close.
     */
    @Transactional(readOnly = true)
    public <T extends AbstractLedgerTxn> BigDecimal yearDeposits(
            LedgerTxnRepository<T> repo, Long memberId, Long yearId) {
        BigDecimal sum = Money.ZERO;
        for (T t : repo.findByMember_IdAndFinancialYear_IdOrderByIdAsc(memberId, yearId)) {
            if (!t.isOpening()) {
                sum = Money.add(sum, Money.subtract(t.getCr(), t.getDr()));
            }
        }
        return sum;
    }

    @Transactional
    public <T extends AbstractLedgerTxn> T addEntry(
            LedgerTxnRepository<T> repo, Supplier<T> factory, Member member, FinancialYear year,
            LocalDate date, BigDecimal dr, BigDecimal cr, String particulars) {
        requireOpen(year);
        BigDecimal current = closingBalance(repo, member.getId(), year.getId());
        BigDecimal drv = Money.scale(dr);
        BigDecimal crv = Money.scale(cr);

        T txn = factory.get();
        txn.setMember(member);
        txn.setFinancialYear(year);
        txn.setTxnDate(date);
        txn.setDr(drv);
        txn.setCr(crv);
        // Compulsory deposit holds its balance frozen at the carried-forward opening
        // for the whole year: monthly deposits accumulate in the Cr column and only
        // fold into the balance at year-end (see YearEndService). Other ledgers run
        // a normal running balance. {@code current} is the frozen balance for CD
        // because every prior CD row keeps it unchanged.
        txn.setBalanceAfter(isConstantBalance(txn)
                ? current
                : Money.add(current, Money.subtract(crv, drv)));
        txn.setParticulars(particulars);
        txn.setOpening(false);
        return repo.save(txn);
    }

    /** Creates the carried-forward opening row for a new year (dr=cr=0). */
    @Transactional
    public <T extends AbstractLedgerTxn> T addOpening(
            LedgerTxnRepository<T> repo, Supplier<T> factory, Member member, FinancialYear year,
            BigDecimal openingBalance) {
        T txn = factory.get();
        txn.setMember(member);
        txn.setFinancialYear(year);
        txn.setTxnDate(year.getStartDate());
        txn.setDr(Money.ZERO);
        txn.setCr(Money.ZERO);
        txn.setBalanceAfter(Money.scale(openingBalance));
        txn.setParticulars("Opening balance (carried forward)");
        txn.setOpening(true);
        return repo.save(txn);
    }

    @Transactional
    public <T extends AbstractLedgerTxn> T updateEntry(
            LedgerTxnRepository<T> repo, Long entryId, LocalDate date, BigDecimal dr, BigDecimal cr,
            String particulars) {
        T txn = repo.findById(entryId)
                .orElseThrow(() -> new NotFoundException("Ledger entry not found: " + entryId));
        requireOpen(txn.getFinancialYear());
        if (txn.isOpening()) {
            throw new ConflictException("Opening (carry-forward) rows cannot be edited directly");
        }
        txn.setTxnDate(date);
        txn.setDr(Money.scale(dr));
        txn.setCr(Money.scale(cr));
        txn.setParticulars(particulars);
        repo.save(txn);
        recompute(repo, txn.getMember().getId(), txn.getFinancialYear().getId());
        return txn;
    }

    @Transactional
    public <T extends AbstractLedgerTxn> void deleteEntry(LedgerTxnRepository<T> repo, Long entryId) {
        T txn = repo.findById(entryId)
                .orElseThrow(() -> new NotFoundException("Ledger entry not found: " + entryId));
        requireOpen(txn.getFinancialYear());
        if (txn.isOpening()) {
            throw new ConflictException("Opening (carry-forward) rows cannot be deleted directly");
        }
        Long memberId = txn.getMember().getId();
        Long yearId = txn.getFinancialYear().getId();
        repo.delete(txn);
        recompute(repo, memberId, yearId);
    }

    /** Re-runs running balances in insertion order; opening rows are the base. */
    @Transactional
    public <T extends AbstractLedgerTxn> void recompute(LedgerTxnRepository<T> repo, Long memberId, Long yearId) {
        List<T> entries = repo.findByMember_IdAndFinancialYear_IdOrderByIdAsc(memberId, yearId);
        boolean constant = !entries.isEmpty() && isConstantBalance(entries.get(0));
        BigDecimal running = Money.ZERO;
        for (T t : entries) {
            if (t.isOpening()) {
                running = Money.scale(t.getBalanceAfter());
            } else if (constant) {
                // Frozen: in-year rows keep the opening balance (deposits fold in at year-end).
                t.setBalanceAfter(running);
            } else {
                running = Money.add(running, Money.subtract(t.getCr(), t.getDr()));
                t.setBalanceAfter(running);
            }
        }
        repo.saveAll(entries);
    }

    /**
     * Whether a ledger holds its balance frozen at the opening for the whole year
     * instead of running it per transaction. Only compulsory deposit does this.
     */
    private boolean isConstantBalance(AbstractLedgerTxn txn) {
        return txn instanceof CompulsoryDepositTxn;
    }

    private void requireOpen(FinancialYear year) {
        if (year.isClosed()) {
            throw new ConflictException(
                    "Financial year " + year.getLabel() + " is closed; entries cannot be changed");
        }
    }
}
