package com.esicsociety.ams.ledger;

import com.esicsociety.ams.common.Money;
import com.esicsociety.ams.financialyear.FinancialYear;
import com.esicsociety.ams.financialyear.FinancialYearService;
import com.esicsociety.ams.ledger.dto.LedgerDtos;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.math.BigDecimal;
import java.util.function.Supplier;

/**
 * Shared REST endpoints for a Dr/Cr/Balance ledger section. Reads are allowed for
 * the owner or an admin; all writes are admin-only. Subclasses bind to
 * {@code /members/{memberId}/<section>} and supply the repository + entity factory.
 */
public abstract class AbstractLedgerController<T extends AbstractLedgerTxn> {

    protected final LedgerService ledgerService;
    protected final MemberService memberService;
    protected final FinancialYearService yearService;

    protected AbstractLedgerController(LedgerService ledgerService, MemberService memberService,
                                       FinancialYearService yearService) {
        this.ledgerService = ledgerService;
        this.memberService = memberService;
        this.yearService = yearService;
    }

    protected abstract LedgerTxnRepository<T> repo();
    protected abstract Supplier<T> factory();
    protected abstract String section();

    /** Default Cr to use when the request omits it (compulsory deposit overrides this). */
    protected BigDecimal defaultCr(Member member) {
        return Money.ZERO;
    }

    @GetMapping
    public LedgerDtos.LedgerView view(@PathVariable Long memberId,
                                      @RequestParam(required = false) Long yearId) {
        CurrentUser.requireOwnerOrAdmin(memberId);
        memberService.getMember(memberId);
        FinancialYear year = resolveYear(yearId);
        return ledgerService.view(repo(), section(), memberId, year.getId());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public LedgerDtos.EntryResponse add(@PathVariable Long memberId,
                                        @RequestParam(required = false) Long yearId,
                                        @Valid @RequestBody LedgerDtos.EntryRequest req) {
        Member member = memberService.getMember(memberId);
        FinancialYear year = resolveYear(yearId);
        BigDecimal dr = req.dr() != null ? req.dr() : Money.ZERO;
        BigDecimal cr = req.cr() != null ? req.cr() : defaultCr(member);
        T saved = ledgerService.addEntry(repo(), factory(), member, year,
                req.txnDate(), dr, cr, req.particulars());
        return LedgerDtos.EntryResponse.of(saved);
    }

    @PutMapping("/{entryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public LedgerDtos.EntryResponse update(@PathVariable Long memberId,
                                           @PathVariable Long entryId,
                                           @Valid @RequestBody LedgerDtos.EntryRequest req) {
        BigDecimal dr = req.dr() != null ? req.dr() : Money.ZERO;
        BigDecimal cr = req.cr() != null ? req.cr() : Money.ZERO;
        T updated = ledgerService.updateEntry(repo(), entryId, req.txnDate(), dr, cr, req.particulars());
        return LedgerDtos.EntryResponse.of(updated);
    }

    @DeleteMapping("/{entryId}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long memberId, @PathVariable Long entryId) {
        ledgerService.deleteEntry(repo(), entryId);
    }

    protected FinancialYear resolveYear(Long yearId) {
        return yearId != null ? yearService.getById(yearId) : yearService.currentYear();
    }
}
