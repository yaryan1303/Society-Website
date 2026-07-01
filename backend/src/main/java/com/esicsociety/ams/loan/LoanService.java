package com.esicsociety.ams.loan;

import com.esicsociety.ams.common.LoanTxnType;
import com.esicsociety.ams.common.Money;
import com.esicsociety.ams.common.PaymentMode;
import com.esicsociety.ams.common.exception.ApiExceptions.BadRequestException;
import com.esicsociety.ams.common.exception.ApiExceptions.ConflictException;
import com.esicsociety.ams.common.exception.ApiExceptions.NotFoundException;
import com.esicsociety.ams.financialyear.FinancialYear;
import com.esicsociety.ams.loan.dto.LoanDtos;
import com.esicsociety.ams.member.Member;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Loan + interest engine. Every mutation appends a {@link LoanTxn} and then
 * {@link #replay(Loan)} recomputes all running balances and interest charges in
 * insertion order, so the ledger is always internally consistent (and edits to
 * an earlier repayment correctly ripple into later interest).
 */
@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final LoanTxnRepository txnRepository;
    private final InterestCalculator interestCalculator;

    public LoanService(LoanRepository loanRepository, LoanTxnRepository txnRepository,
                       InterestCalculator interestCalculator) {
        this.loanRepository = loanRepository;
        this.txnRepository = txnRepository;
        this.interestCalculator = interestCalculator;
    }

    @Transactional(readOnly = true)
    public List<Loan> listLoans(Long memberId) {
        return loanRepository.findByMember_IdOrderByOpeningDateAscIdAsc(memberId);
    }

    @Transactional(readOnly = true)
    public Loan getLoan(Long loanId) {
        return loanRepository.findById(loanId)
                .orElseThrow(() -> new NotFoundException("Loan not found: " + loanId));
    }

    @Transactional(readOnly = true)
    public List<LoanTxn> txnsForLoan(Long loanId) {
        return txnRepository.findByLoan_IdOrderByIdAsc(loanId);
    }

    @Transactional(readOnly = true)
    public List<LoanTxn> txnsForMemberYear(Long memberId, Long yearId) {
        return txnRepository.findByLoan_Member_IdAndFinancialYear_IdOrderByTxnDateAscIdAsc(memberId, yearId);
    }

    public BigDecimal annualRatePct() {
        return interestCalculator.annualRatePct();
    }

    public BigDecimal projectedMonthlyInterest(Loan loan) {
        return loan.isClosed() ? Money.ZERO : interestCalculator.monthlyInterest(loan.getPrincipalOutstanding());
    }

    @Transactional
    public Loan disburse(Member member, FinancialYear year, LoanDtos.DisburseRequest req) {
        requireOpen(year);
        BigDecimal amount = Money.scale(req.amount());

        // Single-loan rule: a member keeps one active loan. An additional loan tops
        // up the existing active loan (raising the total principal) rather than
        // creating a separate record. Only the first loan creates a new record.
        List<Loan> active = loanRepository.findByMember_IdAndClosedFalseOrderByOpeningDateAscIdAsc(member.getId());
        Loan loan;
        String note;
        if (active.isEmpty()) {
            loan = new Loan();
            loan.setMember(member);
            loan.setBondNo(req.bondNo());
            loan.setConditionOfRepayment(req.conditionOfRepayment());
            loan.setSureties(req.sureties());
            loan.setOpeningDate(req.openingDate());
            loan.setPrincipalAmount(amount);
            loan.setPrincipalOutstanding(Money.ZERO);
            loan.setInterestOutstanding(Money.ZERO);
            loan.setClosed(false);
            loan = loanRepository.save(loan);
            note = "Loan disbursed";
        } else {
            loan = active.get(0);
            loan.setPrincipalAmount(Money.add(loan.getPrincipalAmount(), amount));
            // Refresh loan details only when the additional draw supplies them.
            if (notBlank(req.bondNo())) loan.setBondNo(req.bondNo());
            if (notBlank(req.conditionOfRepayment())) loan.setConditionOfRepayment(req.conditionOfRepayment());
            if (notBlank(req.sureties())) loan.setSureties(req.sureties());
            loanRepository.save(loan);
            note = "Additional loan disbursed";
        }

        LoanTxn txn = baseTxn(loan, year, req.openingDate(), LoanTxnType.DISBURSAL);
        txn.setCbFolio(req.cbFolio());
        txn.setLoanDr(amount);
        txn.setNote(note);
        txnRepository.save(txn);

        replay(loan);
        return loan;
    }

    /**
     * Charge interest for every unpaid month up to {@code txnDate}, one row per
     * month (interest starts the month after disbursal). Returns the rows created.
     */
    @Transactional
    public List<LoanTxn> postInterest(Long loanId, FinancialYear year, LoanDtos.PostInterestRequest req) {
        Loan loan = getLoan(loanId);
        requireOpen(year);
        if (loan.isClosed()) {
            throw new ConflictException("Loan is closed; no further interest can be charged");
        }
        List<LoanTxn> created = accrueInterestUpTo(loan, year, req.txnDate(), req.cbFolio());
        if (created.isEmpty()) {
            throw new BadRequestException(
                    "Interest is already posted up to " + YearMonth.from(req.txnDate()));
        }
        return created;
    }

    /**
     * Charge monthly interest for each unpaid month from the month after the loan's
     * last interest charge — or, if none yet, the month after disbursal (the
     * disbursal month is interest-free) — up to and including the month of
     * {@code upTo}. A loan carried in from a prior year accrues from this year's
     * first month. One INTEREST_CHARGE row per month, bounded to {@code year};
     * {@link #replay(Loan)} then computes each charge on the balance at that point.
     * Returns the rows created (empty if already up to date).
     */
    private List<LoanTxn> accrueInterestUpTo(Loan loan, FinancialYear year, LocalDate upTo, String cbFolio) {
        YearMonth lastCharged = null;
        boolean carriedOver = false;
        for (LoanTxn t : txnRepository.findByLoan_IdOrderByIdAsc(loan.getId())) {
            if (!t.getFinancialYear().getId().equals(year.getId())) {
                continue;
            }
            if (t.getTxnType() == LoanTxnType.INTEREST_CHARGE) {
                YearMonth m = YearMonth.from(t.getTxnDate());
                if (lastCharged == null || m.isAfter(lastCharged)) {
                    lastCharged = m;
                }
            } else if (t.getTxnType() == LoanTxnType.OPENING) {
                carriedOver = true;
            }
        }

        YearMonth baseline;
        if (lastCharged != null) {
            baseline = lastCharged;
        } else if (carriedOver) {
            baseline = YearMonth.from(year.getStartDate()).minusMonths(1); // accrue every month this year
        } else {
            baseline = YearMonth.from(loan.getOpeningDate());              // disbursal month is free
        }

        YearMonth target = YearMonth.from(upTo);
        YearMonth fyEnd = YearMonth.from(year.getEndDate());
        if (target.isAfter(fyEnd)) {
            target = fyEnd;
        }

        List<LoanTxn> created = new ArrayList<>();
        for (YearMonth m = baseline.plusMonths(1); !m.isAfter(target); m = m.plusMonths(1)) {
            LocalDate when = m.atDay(1);
            if (when.isBefore(year.getStartDate())) when = year.getStartDate();
            if (when.isAfter(upTo)) when = upTo;
            LoanTxn t = baseTxn(loan, year, when, LoanTxnType.INTEREST_CHARGE);
            t.setCbFolio(cbFolio);
            t.setNote("Monthly interest @ " + interestCalculator.annualRatePct() + "% p.a. (" + m + ")");
            created.add(txnRepository.save(t));
        }
        if (!created.isEmpty()) {
            replay(loan);
        }
        return created;
    }

    /**
     * Record a repayment from the total {@code amount} paid. The split into
     * interest (all pending interest first) and principal is derived in
     * {@link #replay(Loan)} so it stays correct even if earlier rows change.
     */
    @Transactional
    public LoanTxn repay(Long loanId, FinancialYear year, LoanDtos.RepaymentRequest req) {
        Loan loan = getLoan(loanId);
        requireOpen(year);
        if (loan.isClosed()) {
            throw new ConflictException("Loan is closed; no further repayments can be recorded");
        }

        // Auto-charge interest for any unpaid months up to the payment date first,
        // so the payment clears all pending interest before touching principal.
        accrueInterestUpTo(loan, year, req.txnDate(), req.cbFolio());

        BigDecimal amount = Money.scale(req.amount());
        if (!Money.isPositive(amount)) {
            throw new BadRequestException("Repayment amount must be greater than zero");
        }
        BigDecimal totalOwed = Money.add(loan.getPrincipalOutstanding(), loan.getInterestOutstanding());
        if (Money.gt(amount, totalOwed)) {
            throw new BadRequestException("Amount (" + amount + ") exceeds the total outstanding (interest "
                    + loan.getInterestOutstanding() + " + principal " + loan.getPrincipalOutstanding()
                    + " = " + totalOwed + ")");
        }
        // Backend enforcement: a CASH payment must carry a receipt number.
        if (req.paymentMode() == PaymentMode.CASH && (req.receiptNo() == null || req.receiptNo().isBlank())) {
            throw new BadRequestException("Receipt number is required for CASH payments");
        }

        // Preview split for the note; replay derives the authoritative figures.
        BigDecimal interestPart = amount.min(loan.getInterestOutstanding());
        BigDecimal principalPart = Money.subtract(amount, interestPart);

        LoanTxn txn = baseTxn(loan, year, req.txnDate(), LoanTxnType.REPAYMENT);
        txn.setCbFolio(req.cbFolio());
        txn.setPaymentAmount(amount);
        txn.setPaymentMode(req.paymentMode());
        txn.setReceiptNo(req.receiptNo() != null && !req.receiptNo().isBlank() ? req.receiptNo().trim() : null);
        txn.setNote("Repayment " + amount + " (interest " + interestPart + " + principal " + principalPart + ")");
        txnRepository.save(txn);

        replay(loan);
        return txn;
    }

    /** Delete a single loan transaction and re-derive balances. */
    @Transactional
    public void deleteTxn(Long loanId, Long txnId) {
        Loan loan = getLoan(loanId);
        LoanTxn txn = txnRepository.findById(txnId)
                .orElseThrow(() -> new NotFoundException("Loan transaction not found: " + txnId));
        if (!txn.getLoan().getId().equals(loan.getId())) {
            throw new BadRequestException("Transaction does not belong to this loan");
        }
        requireOpen(txn.getFinancialYear());
        if (txn.getTxnType() == LoanTxnType.OPENING) {
            throw new ConflictException("Opening (carry-forward) rows cannot be deleted directly");
        }
        txnRepository.delete(txn);
        replay(loan);
    }

    @Transactional
    public Loan closeLoan(Long loanId) {
        Loan loan = getLoan(loanId);
        if (Money.isPositive(loan.getPrincipalOutstanding()) || Money.isPositive(loan.getInterestOutstanding())) {
            throw new BadRequestException("Loan cannot be closed while principal or interest is outstanding");
        }
        loan.setClosed(true);
        return loanRepository.save(loan);
    }

    /**
     * Re-derive every running balance + interest charge for a loan from its
     * transactions, in insertion order. This is the single source of truth for
     * loan/interest balances. Throws if an edit would drive a balance negative.
     */
    @Transactional
    public void replay(Loan loan) {
        List<LoanTxn> txns = txnRepository.findByLoan_IdOrderByIdAsc(loan.getId());
        BigDecimal principal = Money.ZERO;
        BigDecimal interest = Money.ZERO;

        for (LoanTxn t : txns) {
            switch (t.getTxnType()) {
                case OPENING -> {
                    // Authoritative carried-forward balances set at year-end.
                    principal = Money.scale(t.getLoanBalanceAfter());
                    interest = Money.scale(t.getInterestBalanceAfter());
                    t.setInterestCharged(Money.ZERO);
                    t.setInterestPaid(Money.ZERO);
                    t.setLoanDr(Money.ZERO);
                    t.setLoanCr(Money.ZERO);
                }
                case DISBURSAL -> {
                    principal = Money.add(principal, t.getLoanDr());
                    t.setInterestCharged(Money.ZERO);
                    t.setInterestPaid(Money.ZERO);
                    t.setLoanCr(Money.ZERO);
                }
                case INTEREST_CHARGE -> {
                    BigDecimal charge = interestCalculator.monthlyInterest(principal);
                    t.setInterestCharged(charge);
                    t.setInterestPaid(Money.ZERO);
                    t.setLoanDr(Money.ZERO);
                    t.setLoanCr(Money.ZERO);
                    interest = Money.add(interest, charge);
                }
                case REPAYMENT -> {
                    // Auto-split the total paid: clear all pending interest first,
                    // then apply the remainder to principal.
                    BigDecimal amount = Money.scale(t.getPaymentAmount());
                    BigDecimal payInterest = amount.min(interest);
                    if (Money.isNegative(payInterest)) payInterest = Money.ZERO;
                    BigDecimal payPrincipal = Money.subtract(amount, payInterest);
                    interest = Money.subtract(interest, payInterest);
                    principal = Money.subtract(principal, payPrincipal);
                    t.setInterestPaid(payInterest);
                    t.setLoanCr(payPrincipal);
                    t.setInterestCharged(Money.ZERO);
                }
            }
            if (Money.isNegative(principal) || Money.isNegative(interest)) {
                throw new BadRequestException(
                        "This change would drive the loan/interest balance negative");
            }
            t.setLoanBalanceAfter(principal);
            t.setInterestBalanceAfter(interest);
        }

        txnRepository.saveAll(txns);
        loan.setPrincipalOutstanding(principal);
        loan.setInterestOutstanding(interest);
        loanRepository.save(loan);
    }

    private LoanTxn baseTxn(Loan loan, FinancialYear year, java.time.LocalDate date, LoanTxnType type) {
        LoanTxn txn = new LoanTxn();
        txn.setLoan(loan);
        txn.setFinancialYear(year);
        txn.setTxnDate(date);
        txn.setTxnType(type);
        txn.setLoanDr(Money.ZERO);
        txn.setLoanCr(Money.ZERO);
        txn.setLoanBalanceAfter(Money.ZERO);
        txn.setInterestCharged(Money.ZERO);
        txn.setInterestPaid(Money.ZERO);
        txn.setInterestBalanceAfter(Money.ZERO);
        return txn;
    }

    private void requireOpen(FinancialYear year) {
        if (year.isClosed()) {
            throw new ConflictException(
                    "Financial year " + year.getLabel() + " is closed; entries cannot be changed");
        }
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
