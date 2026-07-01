package com.esicsociety.ams.ledger;

import com.esicsociety.ams.financialyear.FinancialYearService;
import com.esicsociety.ams.member.Member;
import com.esicsociety.ams.member.MemberService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.function.Supplier;

@RestController
@RequestMapping("/members/{memberId}/compulsory-deposits")
public class CompulsoryDepositController extends AbstractLedgerController<CompulsoryDepositTxn> {

    private final CompulsoryDepositTxnRepository repository;

    public CompulsoryDepositController(LedgerService ledgerService, MemberService memberService,
                                       FinancialYearService yearService,
                                       CompulsoryDepositTxnRepository repository) {
        super(ledgerService, memberService, yearService);
        this.repository = repository;
    }

    @Override protected LedgerTxnRepository<CompulsoryDepositTxn> repo() { return repository; }
    @Override protected Supplier<CompulsoryDepositTxn> factory() { return CompulsoryDepositTxn::new; }
    @Override protected String section() { return "COMPULSORY_DEPOSIT"; }

    /** Fixed monthly deposit: default the Cr to the member's configured amount. */
    @Override
    protected BigDecimal defaultCr(Member member) {
        return member.getCompulsoryDepositAmount();
    }
}
