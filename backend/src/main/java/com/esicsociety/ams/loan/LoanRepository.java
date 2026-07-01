package com.esicsociety.ams.loan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByMember_IdOrderByOpeningDateAscIdAsc(Long memberId);

    List<Loan> findByMember_IdAndClosedFalseOrderByOpeningDateAscIdAsc(Long memberId);
}
