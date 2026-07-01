package com.esicsociety.ams.config;

import com.esicsociety.ams.common.PaymentMode;
import com.esicsociety.ams.common.Role;
import com.esicsociety.ams.financialyear.FinancialYear;
import com.esicsociety.ams.financialyear.FinancialYearService;
import com.esicsociety.ams.ledger.CompulsoryDepositTxn;
import com.esicsociety.ams.ledger.CompulsoryDepositTxnRepository;
import com.esicsociety.ams.ledger.LedgerService;
import com.esicsociety.ams.ledger.OtherDepositTxn;
import com.esicsociety.ams.ledger.OtherDepositTxnRepository;
import com.esicsociety.ams.ledger.ShareTxn;
import com.esicsociety.ams.ledger.ShareTxnRepository;
import com.esicsociety.ams.loan.Loan;
import com.esicsociety.ams.loan.LoanService;
import com.esicsociety.ams.loan.dto.LoanDtos;
import com.esicsociety.ams.member.Member;
import com.esicsociety.ams.member.MemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Seeds a known ADMIN account and (optionally) the demo member "Yogesh Kr. Yadav"
 * (A/c 1583) with sample ledger data that reproduces the figures on the reference
 * ledger photo, so the client can click through immediately. Idempotent.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String DEMO_ACCOUNT_NO = "1583";
    private static final String DEMO_PASSWORD = "Demo@12345";

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties props;
    private final FinancialYearService yearService;
    private final LedgerService ledgerService;
    private final LoanService loanService;
    private final ShareTxnRepository shareRepo;
    private final CompulsoryDepositTxnRepository cdRepo;
    private final OtherDepositTxnRepository odRepo;

    public DataSeeder(MemberRepository memberRepository, PasswordEncoder passwordEncoder,
                      AppProperties props, FinancialYearService yearService, LedgerService ledgerService,
                      LoanService loanService, ShareTxnRepository shareRepo,
                      CompulsoryDepositTxnRepository cdRepo, OtherDepositTxnRepository odRepo) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
        this.yearService = yearService;
        this.ledgerService = ledgerService;
        this.loanService = loanService;
        this.shareRepo = shareRepo;
        this.cdRepo = cdRepo;
        this.odRepo = odRepo;
    }

    @Override
    @Transactional
    public void run(String... args) {
        FinancialYear year = ensureCurrentYear();
        seedAdmin();
        if (props.isSeedDemo()) {
            seedDemoMember(year);
        }
    }

    private FinancialYear ensureCurrentYear() {
        LocalDate today = LocalDate.now();
        int startYear = today.getMonthValue() >= 4 ? today.getYear() : today.getYear() - 1;
        FinancialYear year = yearService.ensureYear(startYear);
        log.info("Current financial year: {}", year.getLabel());
        return year;
    }

    private void seedAdmin() {
        String accountNo = props.getAdmin().getAccountNo();
        if (memberRepository.existsByAccountNo(accountNo)) {
            return;
        }
        Member admin = new Member();
        admin.setAccountNo(accountNo);
        admin.setName("Administrator");
        admin.setEmail(props.getAdmin().getEmail());
        admin.setRole(Role.ADMIN);
        admin.setPasswordHash(passwordEncoder.encode(props.getAdmin().getPassword()));
        admin.setMustChangePassword(false);
        admin.setActive(true);
        admin.setCompulsoryDepositAmount(BigDecimal.ZERO);
        memberRepository.save(admin);
        log.info("Seeded ADMIN account '{}'. Change the password after first login.", accountNo);
    }

    private void seedDemoMember(FinancialYear year) {
        if (memberRepository.existsByAccountNo(DEMO_ACCOUNT_NO)) {
            return;
        }
        Member m = new Member();
        m.setAccountNo(DEMO_ACCOUNT_NO);
        m.setName("Yogesh Kr. Yadav");
        m.setFatherOrHusbandName("—");
        m.setEmail("yogesh.demo@esicsociety.local");
        m.setRole(Role.MEMBER);
        m.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
        // Demo convenience: no forced change so the client can log in directly.
        m.setMustChangePassword(false);
        m.setActive(true);
        m.setCompulsoryDepositAmount(new BigDecimal("1500.00"));
        m.setMaxCreditLimit(new BigDecimal("200000.00"));
        m = memberRepository.save(m);

        LocalDate apr = year.getStartDate();             // 1 Apr
        LocalDate may = apr.plusMonths(1);
        LocalDate jun = apr.plusMonths(2);
        LocalDate jul = apr.plusMonths(3);

        // --- Shares: build up to 28,000 ---
        ledgerService.addEntry(shareRepo, ShareTxn::new, m, year, apr,
                BigDecimal.ZERO, new BigDecimal("23000"), "Opening share holding");
        ledgerService.addEntry(shareRepo, ShareTxn::new, m, year, may,
                BigDecimal.ZERO, new BigDecimal("5000"), "Shares purchased (R.No 12499)");

        // --- Compulsory deposit: balance frozen at the carried-forward figure for
        //     the whole year (72,942 on the reference ledger photo); the 1,500/month
        //     deposits accrue in the Deposit column and only fold into the balance at
        //     year-end (minus any favour-stood split). ---
        ledgerService.addOpening(cdRepo, CompulsoryDepositTxn::new, m, year, new BigDecimal("72942"));
        for (LocalDate d : new LocalDate[]{apr, may, jun}) {
            ledgerService.addEntry(cdRepo, CompulsoryDepositTxn::new, m, year, d,
                    BigDecimal.ZERO, m.getCompulsoryDepositAmount(), "Monthly compulsory deposit");
        }

        // --- Other deposit: 1,000 ---
        ledgerService.addEntry(odRepo, OtherDepositTxn::new, m, year, apr,
                BigDecimal.ZERO, new BigDecimal("1000"), "Other deposit");

        // --- Loan reproducing the reference figures (1,83,000 -> 1220, etc.) ---
        // Interest starts the month after disbursal (April is free). Each repayment
        // auto-charges that month's interest, then the total splits interest-first.
        Loan loan = loanService.disburse(m, year, new LoanDtos.DisburseRequest(
                apr, new BigDecimal("183000"), "BOND-1583", "Monthly repayment",
                "Sona Kumar Singh, Umesh Chaurasia", "CB-12"));

        // May: accrues May interest 1220 (on 1,83,000), then 10,000 to principal -> 1,73,000.
        loanService.repay(loan.getId(), year, new LoanDtos.RepaymentRequest(
                may, new BigDecimal("11220"), PaymentMode.CASH, "R-1001", "CB-13"));
        // June: accrues June interest 1153 (on 1,73,000), then 10,000 to principal -> 1,63,000.
        loanService.repay(loan.getId(), year, new LoanDtos.RepaymentRequest(
                jun, new BigDecimal("11153"), PaymentMode.BANK, null, "CB-14"));
        // July: leave one month's interest (1087 on 1,63,000) outstanding to show accrual.
        loanService.postInterest(loan.getId(), year, new LoanDtos.PostInterestRequest(jul, "CB-15"));

        log.info("Seeded demo member '{}' (A/c {}) with sample ledger data. Demo password: {}",
                m.getName(), DEMO_ACCOUNT_NO, DEMO_PASSWORD);
    }
}
