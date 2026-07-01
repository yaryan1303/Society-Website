package com.esicsociety.ams.loan;

import com.esicsociety.ams.common.BaseEntity;
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
 * A loan held by a member. Members may hold several concurrent loans; each one
 * keeps its own outstanding principal and accumulated interest balance. Monthly
 * interest is charged on {@code principalOutstanding} (reducing balance).
 */
@Entity
@Table(name = "loan")
@Getter
@Setter
public class Loan extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "bond_no", length = 64)
    private String bondNo;

    @Column(name = "condition_of_repayment", length = 255)
    private String conditionOfRepayment;

    /** Names of the member(s) standing as guarantor. */
    @Column(length = 255)
    private String sureties;

    @Column(name = "opening_date", nullable = false)
    private LocalDate openingDate;

    @Column(name = "principal_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal principalAmount = BigDecimal.ZERO;

    @Column(name = "principal_outstanding", nullable = false, precision = 15, scale = 2)
    private BigDecimal principalOutstanding = BigDecimal.ZERO;

    @Column(name = "interest_outstanding", nullable = false, precision = 15, scale = 2)
    private BigDecimal interestOutstanding = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean closed = false;
}
