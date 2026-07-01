package com.esicsociety.ams.financialyear;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FinancialYearRepository extends JpaRepository<FinancialYear, Long> {

    Optional<FinancialYear> findByLabel(String label);

    boolean existsByLabel(String label);

    List<FinancialYear> findAllByOrderByStartDateDesc();

    Optional<FinancialYear> findFirstByClosedFalseOrderByStartDateAsc();
}
