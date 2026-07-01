package com.esicsociety.ams.yearend;

import com.esicsociety.ams.common.LoanTxnType;
import com.esicsociety.ams.common.Money;
import com.esicsociety.ams.common.Role;
import com.esicsociety.ams.common.exception.ApiExceptions.BadRequestException;
import com.esicsociety.ams.common.exception.ApiExceptions.ConflictException;
import com.esicsociety.ams.favourstood.FavourStoodService;
import com.esicsociety.ams.financialyear.FinancialYear;
import com.esicsociety.ams.financialyear.FinancialYearRepository;
import com.esicsociety.ams.financialyear.FinancialYearService;
import com.esicsociety.ams.ledger.CompulsoryDepositTxn;
import com.esicsociety.ams.ledger.CompulsoryDepositTxnRepository;
import com.esicsociety.ams.ledger.LedgerService;
import com.esicsociety.ams.ledger.OtherDepositTxn;
import com.esicsociety.ams.ledger.OtherDepositTxnRepository;
import com.esicsociety.ams.ledger.ShareTxn;
import com.esicsociety.ams.ledger.ShareTxnRepository;
import com.esicsociety.ams.loan.Loan;
import com.esicsociety.ams.loan.LoanRepository;
import com.esicsociety.ams.loan.LoanTxn;
import com.esicsociety.ams.loan.LoanTxnRepository;
import com.esicsociety.ams.member.Member;
import com.esicsociety.ams.member.MemberRepository;
import com.esicsociety.ams.yearend.dto.YearEndDtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs the 31-March year-end close: locks the year and carries every closing
 * balance forward as the opening balance of the next year. For compulsory
 * deposits the admin-decided amount is moved into "members in whose favour stood"
 * and the remainder is carried forward.
 */
@Service
public class YearEndService {

    private final MemberRepository memberRepository;
    private final FinancialYearService yearService;
    private final FinancialYearRepository yearRepository;
    private final LedgerService ledgerService;
    private final FavourStoodService favourStoodService;
    private final ShareTxnRepository shareRepo;
    private final CompulsoryDepositTxnRepository cdRepo;
    private final OtherDepositTxnRepository odRepo;
    private final LoanRepository loanRepo;
    private final LoanTxnRepository loanTxnRepo;

    public YearEndService(MemberRepository memberRepository, FinancialYearService yearService,
                          FinancialYearRepository yearRepository, LedgerService ledgerService,
                          FavourStoodService favourStoodService, ShareTxnRepository shareRepo,
                          CompulsoryDepositTxnRepository cdRepo, OtherDepositTxnRepository odRepo,
                          LoanRepository loanRepo, LoanTxnRepository loanTxnRepo) {
        this.memberRepository = memberRepository;
        this.yearService = yearService;
        this.yearRepository = yearRepository;
        this.ledgerService = ledgerService;
        this.favourStoodService = favourStoodService;
        this.shareRepo = shareRepo;
        this.cdRepo = cdRepo;
        this.odRepo = odRepo;
        this.loanRepo = loanRepo;
        this.loanTxnRepo = loanTxnRepo;
    }

    /** Preview of every member's closing balances before the admin confirms the close. */
    @Transactional(readOnly = true)
    public List<YearEndDtos.MemberCarryForward> preview(Long yearId) {
        FinancialYear year = yearService.getById(yearId);
        return memberRepository.findByRoleOrderByNameAsc(Role.MEMBER).stream().map(m -> {
            BigDecimal loanOut = Money.ZERO;
            BigDecimal interestOut = Money.ZERO;
            for (Loan loan : loanRepo.findByMember_IdOrderByOpeningDateAscIdAsc(m.getId())) {
                loanOut = Money.add(loanOut, loan.getPrincipalOutstanding());
                interestOut = Money.add(interestOut, loan.getInterestOutstanding());
            }
            return new YearEndDtos.MemberCarryForward(
                    m.getId(), m.getAccountNo(), m.getName(),
                    ledgerService.closingBalance(shareRepo, m.getId(), year.getId()),
                    ledgerService.closingBalance(cdRepo, m.getId(), year.getId()),
                    ledgerService.yearDeposits(cdRepo, m.getId(), year.getId()),
                    ledgerService.closingBalance(odRepo, m.getId(), year.getId()),
                    loanOut, interestOut);
        }).toList();
    }

    @Transactional
    public YearEndDtos.CloseYearResult closeYear(Long yearId, YearEndDtos.CloseYearRequest req) {
        FinancialYear year = yearService.getById(yearId);
        if (year.isClosed()) {
            throw new ConflictException("Financial year " + year.getLabel() + " is already closed");
        }

        Map<Long, BigDecimal> favourByMember = new HashMap<>();
        if (req != null && req.splits() != null) {
            for (YearEndDtos.FavourStoodSplit s : req.splits()) {
                favourByMember.merge(s.memberId(), Money.scale(s.amount()), Money::add);
            }
        }

        FinancialYear nextYear = yearService.ensureYear(year.getStartDate().getYear() + 1);
        BigDecimal totalFavour = Money.ZERO;
        int processed = 0;

        for (Member member : memberRepository.findByRoleOrderByNameAsc(Role.MEMBER)) {
            Long mid = member.getId();

            // Shares -> opening of next year
            BigDecimal shares = ledgerService.closingBalance(shareRepo, mid, year.getId());
            if (Money.isPositive(shares)) {
                ledgerService.addOpening(shareRepo, ShareTxn::new, member, nextYear, shares);
            }

            // Compulsory deposit: the balance stays frozen at the opening all year;
            // this year's deposits fold in now. The admin may divert part of THIS
            // YEAR'S DEPOSITS to favour-stood; the remainder is added to the frozen
            // balance to form next year's (again frozen) opening balance.
            BigDecimal cdFrozen = ledgerService.closingBalance(cdRepo, mid, year.getId());
            BigDecimal cdDeposits = ledgerService.yearDeposits(cdRepo, mid, year.getId());
            BigDecimal favour = favourByMember.getOrDefault(mid, Money.ZERO);
            if (Money.isNegative(favour)) {
                throw new BadRequestException("Favour-stood amount cannot be negative for member " + mid);
            }
            if (Money.gt(favour, cdDeposits)) {
                throw new BadRequestException("Favour-stood amount (" + favour
                        + ") exceeds this year's compulsory deposits (" + cdDeposits + ") for member "
                        + member.getAccountNo());
            }
            if (Money.isPositive(favour)) {
                favourStoodService.add(member, year, year.getEndDate(), favour,
                        "Year-end deposit split from compulsory deposit");
                totalFavour = Money.add(totalFavour, favour);
            }
            // next opening = frozen balance + (this year's deposits - favour-stood split)
            BigDecimal cdCarry = Money.add(cdFrozen, Money.subtract(cdDeposits, favour));
            if (Money.isPositive(cdCarry)) {
                ledgerService.addOpening(cdRepo, CompulsoryDepositTxn::new, member, nextYear, cdCarry);
            }

            // Favour-stood carries forward cumulatively (this year's entries + any
            // carried opening) so prior years' amounts stay visible next year.
            BigDecimal favourTotal = favourStoodService.total(mid, year.getId());
            if (Money.isPositive(favourTotal)) {
                favourStoodService.addOpening(member, nextYear, nextYear.getStartDate(), favourTotal);
            }

            // Other deposit -> opening of next year
            BigDecimal other = ledgerService.closingBalance(odRepo, mid, year.getId());
            if (Money.isPositive(other)) {
                ledgerService.addOpening(odRepo, OtherDepositTxn::new, member, nextYear, other);
            }

            // Loans: carry outstanding principal + interest as an OPENING loan row
            for (Loan loan : loanRepo.findByMember_IdAndClosedFalseOrderByOpeningDateAscIdAsc(mid)) {
                if (Money.isPositive(loan.getPrincipalOutstanding())
                        || Money.isPositive(loan.getInterestOutstanding())) {
                    LoanTxn opening = new LoanTxn();
                    opening.setLoan(loan);
                    opening.setFinancialYear(nextYear);
                    opening.setTxnDate(nextYear.getStartDate());
                    opening.setTxnType(LoanTxnType.OPENING);
                    opening.setLoanDr(Money.ZERO);
                    opening.setLoanCr(Money.ZERO);
                    opening.setLoanBalanceAfter(loan.getPrincipalOutstanding());
                    opening.setInterestCharged(Money.ZERO);
                    opening.setInterestPaid(Money.ZERO);
                    opening.setInterestBalanceAfter(loan.getInterestOutstanding());
                    opening.setNote("Opening balance (carried forward)");
                    loanTxnRepo.save(opening);
                }
            }
            processed++;
        }

        year.setClosed(true);
        yearRepository.save(year);

        return new YearEndDtos.CloseYearResult(year.getLabel(), nextYear.getLabel(), processed, totalFavour);
    }
}
