package com.esicsociety.ams.member;

import com.esicsociety.ams.common.BaseEntity;
import com.esicsociety.ams.common.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * A society member (or the admin). The {@code accountNo} is the ledger account
 * number printed in the book (e.g. "1583") and is used as the login username.
 */
@Entity
@Table(name = "member")
@Getter
@Setter
public class Member extends BaseEntity {

    @Column(name = "account_no", nullable = false, unique = true, length = 32)
    private String accountNo;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "father_or_husband_name", length = 160)
    private String fatherOrHusbandName;

    @Column(length = 160)
    private String email;

    @Column(length = 255)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Role role = Role.MEMBER;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword = true;

    @Column(nullable = false)
    private boolean active = true;

    /** Fixed monthly compulsory-deposit amount for this member (default 1500). */
    @Column(name = "compulsory_deposit_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal compulsoryDepositAmount = new BigDecimal("1500.00");

    /** Maximum credit limit printed on the ledger; informational. */
    @Column(name = "max_credit_limit", precision = 15, scale = 2)
    private BigDecimal maxCreditLimit;

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
