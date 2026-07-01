package com.esicsociety.ams.favourstood;

import com.esicsociety.ams.common.Money;
import com.esicsociety.ams.common.exception.ApiExceptions.ConflictException;
import com.esicsociety.ams.common.exception.ApiExceptions.NotFoundException;
import com.esicsociety.ams.financialyear.FinancialYear;
import com.esicsociety.ams.member.Member;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class FavourStoodService {

    private final FavourStoodEntryRepository repository;

    public FavourStoodService(FavourStoodEntryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<FavourStoodEntry> list(Long memberId, Long yearId) {
        return repository.findByMember_IdAndFinancialYear_IdOrderByEntryDateAscIdAsc(memberId, yearId);
    }

    /** Cumulative favour-stood total for a member-year (includes any carried opening). */
    @Transactional(readOnly = true)
    public BigDecimal total(Long memberId, Long yearId) {
        return repository.findByMember_IdAndFinancialYear_IdOrderByEntryDateAscIdAsc(memberId, yearId)
                .stream().map(FavourStoodEntry::getAmount).reduce(Money.ZERO, Money::add);
    }

    /**
     * Year-end carry-forward row so prior years' favour-stood stays visible in the
     * next year's tab. Marked {@code opening} and protected from manual deletion.
     */
    @Transactional
    public FavourStoodEntry addOpening(Member member, FinancialYear year, LocalDate date, BigDecimal amount) {
        FavourStoodEntry e = new FavourStoodEntry();
        e.setMember(member);
        e.setFinancialYear(year);
        e.setEntryDate(date);
        e.setAmount(Money.scale(amount));
        e.setNote("Opening balance (carried forward)");
        e.setOpening(true);
        return repository.save(e);
    }

    /** Create an entry (admin). Used both for manual adds and the year-end split. */
    @Transactional
    public FavourStoodEntry add(Member member, FinancialYear year, LocalDate date, BigDecimal amount, String note) {
        FavourStoodEntry e = new FavourStoodEntry();
        e.setMember(member);
        e.setFinancialYear(year);
        e.setEntryDate(date);
        e.setAmount(amount);
        e.setNote(note);
        return repository.save(e);
    }

    /** Manual add via the API guards against a closed year. */
    @Transactional
    public FavourStoodEntry addManual(Member member, FinancialYear year, LocalDate date, BigDecimal amount, String note) {
        if (year.isClosed()) {
            throw new ConflictException("Financial year " + year.getLabel() + " is closed");
        }
        return add(member, year, date, amount, note);
    }

    @Transactional
    public void delete(Long id) {
        FavourStoodEntry e = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Favour-stood entry not found: " + id));
        if (e.isOpening()) {
            throw new ConflictException("Opening (carry-forward) entries cannot be deleted directly");
        }
        if (e.getFinancialYear().isClosed()) {
            throw new ConflictException("Financial year is closed");
        }
        repository.delete(e);
    }
}
