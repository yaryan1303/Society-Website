package com.esicsociety.ams.loan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanTxnRepository extends JpaRepository<LoanTxn, Long> {

    List<LoanTxn> findByLoan_IdOrderByIdAsc(Long loanId);

    List<LoanTxn> findByLoan_Member_IdAndFinancialYear_IdOrderByTxnDateAscIdAsc(Long memberId, Long yearId);

    List<LoanTxn> findByLoan_IdAndFinancialYear_IdOrderByIdAsc(Long loanId, Long yearId);
}
