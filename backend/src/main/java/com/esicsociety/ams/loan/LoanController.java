package com.esicsociety.ams.loan;

import com.esicsociety.ams.common.exception.ApiExceptions.BadRequestException;
import com.esicsociety.ams.financialyear.FinancialYear;
import com.esicsociety.ams.financialyear.FinancialYearService;
import com.esicsociety.ams.loan.dto.LoanDtos;
import com.esicsociety.ams.member.Member;
import com.esicsociety.ams.member.MemberService;
import com.esicsociety.ams.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/members/{memberId}/loans")
public class LoanController {

    private final LoanService loanService;
    private final MemberService memberService;
    private final FinancialYearService yearService;

    public LoanController(LoanService loanService, MemberService memberService,
                          FinancialYearService yearService) {
        this.loanService = loanService;
        this.memberService = memberService;
        this.yearService = yearService;
    }

    @GetMapping
    public List<LoanDtos.LoanResponse> list(@PathVariable Long memberId) {
        CurrentUser.requireOwnerOrAdmin(memberId);
        return loanService.listLoans(memberId).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{loanId}")
    public LoanDtos.LoanLedgerView detail(@PathVariable Long memberId, @PathVariable Long loanId) {
        CurrentUser.requireOwnerOrAdmin(memberId);
        Loan loan = loadOwned(memberId, loanId);
        List<LoanDtos.LoanTxnResponse> txns = loanService.txnsForLoan(loanId).stream()
                .map(LoanDtos.LoanTxnResponse::of).toList();
        return new LoanDtos.LoanLedgerView(toResponse(loan), txns);
    }

    /** All loan transactions for this member in a year (matches the paper loan page). */
    @GetMapping("/txns")
    public List<LoanDtos.LoanTxnResponse> txnsForYear(@PathVariable Long memberId,
                                                      @RequestParam(required = false) Long yearId) {
        CurrentUser.requireOwnerOrAdmin(memberId);
        FinancialYear year = resolveYear(yearId);
        return loanService.txnsForMemberYear(memberId, year.getId()).stream()
                .map(LoanDtos.LoanTxnResponse::of).toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public LoanDtos.LoanResponse disburse(@PathVariable Long memberId,
                                          @RequestParam(required = false) Long yearId,
                                          @Valid @RequestBody LoanDtos.DisburseRequest req) {
        Member member = memberService.getMember(memberId);
        FinancialYear year = resolveYear(yearId);
        return toResponse(loanService.disburse(member, year, req));
    }

    @PostMapping("/{loanId}/interest")
    @PreAuthorize("hasRole('ADMIN')")
    public List<LoanDtos.LoanTxnResponse> postInterest(@PathVariable Long memberId, @PathVariable Long loanId,
                                                       @RequestParam(required = false) Long yearId,
                                                       @Valid @RequestBody LoanDtos.PostInterestRequest req) {
        loadOwned(memberId, loanId);
        FinancialYear year = resolveYear(yearId);
        return loanService.postInterest(loanId, year, req).stream()
                .map(LoanDtos.LoanTxnResponse::of).toList();
    }

    @PostMapping("/{loanId}/repayments")
    @PreAuthorize("hasRole('ADMIN')")
    public LoanDtos.LoanTxnResponse repay(@PathVariable Long memberId, @PathVariable Long loanId,
                                          @RequestParam(required = false) Long yearId,
                                          @Valid @RequestBody LoanDtos.RepaymentRequest req) {
        loadOwned(memberId, loanId);
        FinancialYear year = resolveYear(yearId);
        return LoanDtos.LoanTxnResponse.of(loanService.repay(loanId, year, req));
    }

    @DeleteMapping("/{loanId}/txns/{txnId}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTxn(@PathVariable Long memberId, @PathVariable Long loanId, @PathVariable Long txnId) {
        loadOwned(memberId, loanId);
        loanService.deleteTxn(loanId, txnId);
    }

    @PostMapping("/{loanId}/close")
    @PreAuthorize("hasRole('ADMIN')")
    public LoanDtos.LoanResponse close(@PathVariable Long memberId, @PathVariable Long loanId) {
        loadOwned(memberId, loanId);
        return toResponse(loanService.closeLoan(loanId));
    }

    private Loan loadOwned(Long memberId, Long loanId) {
        Loan loan = loanService.getLoan(loanId);
        if (!loan.getMember().getId().equals(memberId)) {
            throw new BadRequestException("Loan " + loanId + " does not belong to member " + memberId);
        }
        return loan;
    }

    private LoanDtos.LoanResponse toResponse(Loan loan) {
        return LoanDtos.LoanResponse.of(loan, loanService.annualRatePct(),
                loanService.projectedMonthlyInterest(loan));
    }

    private FinancialYear resolveYear(Long yearId) {
        return yearId != null ? yearService.getById(yearId) : yearService.currentYear();
    }
}
