package com.esicsociety.ams.loan.dto;

import com.esicsociety.ams.common.LoanTxnType;
import com.esicsociety.ams.common.PaymentMode;
import com.esicsociety.ams.loan.Loan;
import com.esicsociety.ams.loan.LoanTxn;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class LoanDtos {

    private LoanDtos() {}

    public record DisburseRequest(
            @NotNull LocalDate openingDate,
            @NotNull @Positive BigDecimal amount,
            @Size(max = 64) String bondNo,
            @Size(max = 255) String conditionOfRepayment,
            @Size(max = 255) String sureties,
            @Size(max = 64) String cbFolio) {}

    /** Post one month's interest on the current outstanding balance. */
    public record PostInterestRequest(
            @NotNull LocalDate txnDate,
            @Size(max = 64) String cbFolio) {}

    /**
     * A repayment: the user enters only the total {@code amount} paid; the system
     * splits it into interest (pending interest first) and principal internally.
     */
    public record RepaymentRequest(
            @NotNull LocalDate txnDate,
            @NotNull @Positive BigDecimal amount,
            @NotNull PaymentMode paymentMode,
            @Size(max = 64) String receiptNo,
            @Size(max = 64) String cbFolio) {}

    public record LoanResponse(
            Long id,
            Long memberId,
            String bondNo,
            String conditionOfRepayment,
            String sureties,
            LocalDate openingDate,
            BigDecimal principalAmount,
            BigDecimal principalOutstanding,
            BigDecimal interestOutstanding,
            BigDecimal annualRatePct,
            BigDecimal projectedMonthlyInterest,
            boolean closed) {

        public static LoanResponse of(Loan l, BigDecimal annualRatePct, BigDecimal projectedMonthlyInterest) {
            return new LoanResponse(l.getId(), l.getMember().getId(), l.getBondNo(),
                    l.getConditionOfRepayment(), l.getSureties(), l.getOpeningDate(),
                    l.getPrincipalAmount(), l.getPrincipalOutstanding(), l.getInterestOutstanding(),
                    annualRatePct, projectedMonthlyInterest, l.isClosed());
        }
    }

    public record LoanTxnResponse(
            Long id,
            LocalDate txnDate,
            LoanTxnType txnType,
            String cbFolio,
            BigDecimal loanDr,
            BigDecimal loanCr,
            BigDecimal loanBalanceAfter,
            BigDecimal interestCharged,
            BigDecimal interestPaid,
            BigDecimal interestBalanceAfter,
            PaymentMode paymentMode,
            String receiptNo,
            String note) {

        public static LoanTxnResponse of(LoanTxn t) {
            return new LoanTxnResponse(t.getId(), t.getTxnDate(), t.getTxnType(), t.getCbFolio(),
                    t.getLoanDr(), t.getLoanCr(), t.getLoanBalanceAfter(),
                    t.getInterestCharged(), t.getInterestPaid(), t.getInterestBalanceAfter(),
                    t.getPaymentMode(), t.getReceiptNo(), t.getNote());
        }
    }

    public record LoanLedgerView(LoanResponse loan, List<LoanTxnResponse> txns) {}
}
