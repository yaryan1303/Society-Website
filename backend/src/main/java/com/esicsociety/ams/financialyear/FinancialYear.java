package com.esicsociety.ams.financialyear;

import com.esicsociety.ams.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * An Indian financial year (1 April – 31 March), shared across all members.
 * Per-member ledgers are obtained by filtering transactions on (member, year).
 * Closing a year locks it and carries balances into the next year.
 */
@Entity
@Table(name = "financial_year")
@Getter
@Setter
public class FinancialYear extends BaseEntity {

    /** e.g. "2025-26". */
    @Column(nullable = false, unique = true, length = 9)
    private String label;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private boolean closed = false;

    public static String labelFor(int startYear) {
        int end = (startYear + 1) % 100;
        return "%d-%02d".formatted(startYear, end);
    }
}
