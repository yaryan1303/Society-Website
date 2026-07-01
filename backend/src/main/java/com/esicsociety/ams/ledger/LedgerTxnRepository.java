package com.esicsociety.ams.ledger;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

/**
 * Common base for the share / compulsory-deposit / other-deposit ledgers so a
 * single {@link LedgerService} can drive all three. Entries are always read in
 * insertion (id) order, which defines the running balance.
 */
@NoRepositoryBean
public interface LedgerTxnRepository<T extends AbstractLedgerTxn> extends JpaRepository<T, Long> {

    List<T> findByMember_IdAndFinancialYear_IdOrderByIdAsc(Long memberId, Long yearId);
}
