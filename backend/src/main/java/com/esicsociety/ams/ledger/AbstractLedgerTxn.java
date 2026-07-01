package com.esicsociety.ams.ledger;

import com.esicsociety.ams.common.BaseEntity;
import com.esicsociety.ams.financialyear.FinancialYear;
import com.esicsociety.ams.member.Member;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Shared shape for the Dr / Cr / running-Balance ledgers (shares + both
 * deposits). {@code balanceAfter} is always computed server-side and never
 * trusted from the client. {@code opening=true} marks a carry-forward row
 * created at year-end (dr=cr=0, balanceAfter = carried balance).
 */
@MappedSuperclass
@Getter
@Setter
public abstract class AbstractLedgerTxn extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "year_id", nullable = false)
    private FinancialYear financialYear;

    @Column(name = "txn_date", nullable = false)
    private LocalDate txnDate;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal dr = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal cr = BigDecimal.ZERO;

    @Column(name = "balance_after", nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceAfter = BigDecimal.ZERO;

    @Column(length = 255)
    private String particulars;

    @Column(nullable = false)
    private boolean opening = false;
}
