package com.esicsociety.ams.auth;

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

import java.time.LocalDateTime;

/** Single-use, time-limited token for the email-based password reset flow. */
@Entity
@Table(name = "password_reset_token")
@Getter
@Setter
public class PasswordResetToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, unique = true, length = 128)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    public boolean isValid() {
        return !used && expiresAt.isAfter(LocalDateTime.now());
    }
}
