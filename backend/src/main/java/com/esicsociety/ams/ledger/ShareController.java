package com.esicsociety.ams.ledger;

import com.esicsociety.ams.financialyear.FinancialYearService;
import com.esicsociety.ams.member.MemberService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Supplier;

@RestController
@RequestMapping("/members/{memberId}/shares")
public class ShareController extends AbstractLedgerController<ShareTxn> {

    private final ShareTxnRepository repository;

    public ShareController(LedgerService ledgerService, MemberService memberService,
                           FinancialYearService yearService, ShareTxnRepository repository) {
        super(ledgerService, memberService, yearService);
        this.repository = repository;
    }

    @Override protected LedgerTxnRepository<ShareTxn> repo() { return repository; }
    @Override protected Supplier<ShareTxn> factory() { return ShareTxn::new; }
    @Override protected String section() { return "SHARES"; }
}
