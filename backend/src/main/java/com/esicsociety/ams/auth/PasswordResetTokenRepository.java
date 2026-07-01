package com.esicsociety.ams.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    @Modifying
    @Query("update PasswordResetToken t set t.used = true where t.member.id = :memberId and t.used = false")
    void invalidateAllForMember(@Param("memberId") Long memberId);
}
