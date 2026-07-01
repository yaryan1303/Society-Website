package com.esicsociety.ams.loan;

import com.esicsociety.ams.common.BaseEntity;
import com.esicsociety.ams.common.LoanTxnType;
import com.esicsociety.ams.common.PaymentMode;
import com.esicsociety.ams.financialyear.FinancialYear;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One row of the loan ledger, mirroring the paper book's "Particulars of Loan"
 * + "Interest" columns. Which fields are populated depends on {@link LoanTxnType}:
 * a disbursal sets loanDr; a monthly charge sets interestCharged; a repayment
 * sets loanCr (principal) and interestPaid. All balances are server-computed.
 */
@Entity
@Table(name = "loan_txn")
@Getter
@Setter
public class LoanTxn extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "year_id", nullable = false)
    private FinancialYear financialYear;

    @Column(name = "txn_date", nullable = false)
    private LocalDate txnDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "txn_type", nullable = false, length = 20)
    private LoanTxnType txnType;

    @Column(name = "cb_folio", length = 64)
    private String cbFolio;

    // --- Loan (principal) columns ---
    @Column(name = "loan_dr", nullable = false, precision = 15, scale = 2)
    private BigDecimal loanDr = BigDecimal.ZERO;

    @Column(name = "loan_cr", nullable = false, precision = 15, scale = 2)
    private BigDecimal loanCr = BigDecimal.ZERO;

    @Column(name = "loan_balance_after", nullable = false, precision = 15, scale = 2)
    private BigDecimal loanBalanceAfter = BigDecimal.ZERO;

    // --- Interest columns ---
    @Column(name = "interest_charged", nullable = false, precision = 15, scale = 2)
    private BigDecimal interestCharged = BigDecimal.ZERO;

    @Column(name = "interest_paid", nullable = false, precision = 15, scale = 2)
    private BigDecimal interestPaid = BigDecimal.ZERO;

    @Column(name = "interest_balance_after", nullable = false, precision = 15, scale = 2)
    private BigDecimal interestBalanceAfter = BigDecimal.ZERO;

    /**
     * Total amount paid on a REPAYMENT row. The split into {@link #interestPaid}
     * (pending interest first) and {@link #loanCr} (principal) is derived from this
     * during replay, so this is the authoritative input for a repayment.
     */
    @Column(name = "payment_amount", precision = 15, scale = 2)
    private BigDecimal paymentAmount;

    // --- Payment metadata (repayments only) ---
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", length = 8)
    private PaymentMode paymentMode;

    @Column(name = "receipt_no", length = 64)
    private String receiptNo;

    @Column(length = 255)
    private String note;
}
