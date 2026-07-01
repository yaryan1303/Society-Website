package com.esicsociety.ams.favourstood;

import com.esicsociety.ams.common.BaseEntity;
import com.esicsociety.ams.financialyear.FinancialYear;
import com.esicsociety.ams.member.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * "Members in whose favour stood" entry. Records only an amount and a date, and
 * only when an amount is credited (per spec §7). This is where the admin-decided
 * year-end compulsory-deposit split lands.
 */
@Entity
@Table(name = "favour_stood_entry")
@Getter
@Setter
public class FavourStoodEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "year_id", nullable = false)
    private FinancialYear financialYear;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 255)
    private String note;

    /** Year-end carry-forward row: the cumulative total brought in from prior years. */
    @Column(nullable = false)
    private boolean opening = false;
}
