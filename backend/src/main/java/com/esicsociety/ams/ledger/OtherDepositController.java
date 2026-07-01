package com.esicsociety.ams.ledger;

import com.esicsociety.ams.financialyear.FinancialYearService;
import com.esicsociety.ams.member.MemberService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Supplier;

@RestController
@RequestMapping("/members/{memberId}/other-deposits")
public class OtherDepositController extends AbstractLedgerController<OtherDepositTxn> {

    private final OtherDepositTxnRepository repository;

    public OtherDepositController(LedgerService ledgerService, MemberService memberService,
                                  FinancialYearService yearService, OtherDepositTxnRepository repository) {
        super(ledgerService, memberService, yearService);
        this.repository = repository;
    }

    @Override protected LedgerTxnRepository<OtherDepositTxn> repo() { return repository; }
    @Override protected Supplier<OtherDepositTxn> factory() { return OtherDepositTxn::new; }
    @Override protected String section() { return "OTHER_DEPOSIT"; }
}
