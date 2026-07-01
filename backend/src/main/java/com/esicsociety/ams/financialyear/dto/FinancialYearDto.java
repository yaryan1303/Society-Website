package com.esicsociety.ams.financialyear.dto;

import com.esicsociety.ams.financialyear.FinancialYear;

import java.time.LocalDate;

public record FinancialYearDto(Long id, String label, LocalDate startDate, LocalDate endDate, boolean closed) {

    public static FinancialYearDto of(FinancialYear y) {
        return new FinancialYearDto(y.getId(), y.getLabel(), y.getStartDate(), y.getEndDate(), y.isClosed());
    }
}
