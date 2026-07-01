package com.esicsociety.ams.financialyear;

import com.esicsociety.ams.common.exception.ApiExceptions.ConflictException;
import com.esicsociety.ams.common.exception.ApiExceptions.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class FinancialYearService {

    private final FinancialYearRepository repository;

    public FinancialYearService(FinancialYearRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<FinancialYear> listAll() {
        return repository.findAllByOrderByStartDateDesc();
    }

    @Transactional(readOnly = true)
    public FinancialYear getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Financial year not found: " + id));
    }

    /**
     * The year a dashboard should default to: the earliest still-open year, or
     * the most recent year if all are closed.
     */
    @Transactional(readOnly = true)
    public FinancialYear currentYear() {
        return repository.findFirstByClosedFalseOrderByStartDateAsc()
                .or(() -> repository.findAllByOrderByStartDateDesc().stream().findFirst())
                .orElseThrow(() -> new NotFoundException("No financial year exists yet"));
    }

    /** Creates a financial year that runs 1 April (startYear) – 31 March (startYear+1). */
    @Transactional
    public FinancialYear create(int startYear) {
        String label = FinancialYear.labelFor(startYear);
        if (repository.existsByLabel(label)) {
            throw new ConflictException("Financial year already exists: " + label);
        }
        return repository.save(build(startYear));
    }

    /** Idempotent: returns the year for startYear, creating it if absent. */
    @Transactional
    public FinancialYear ensureYear(int startYear) {
        String label = FinancialYear.labelFor(startYear);
        return repository.findByLabel(label).orElseGet(() -> repository.save(build(startYear)));
    }

    private FinancialYear build(int startYear) {
        FinancialYear y = new FinancialYear();
        y.setLabel(FinancialYear.labelFor(startYear));
        y.setStartDate(LocalDate.of(startYear, 4, 1));
        y.setEndDate(LocalDate.of(startYear + 1, 3, 31));
        y.setClosed(false);
        return y;
    }
}
