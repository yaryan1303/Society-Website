package com.esicsociety.ams.member.dto;

import com.esicsociety.ams.common.Role;
import com.esicsociety.ams.member.Member;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Request/response DTOs for member management. */
public final class MemberDtos {

    private MemberDtos() {}

    public record CreateRequest(
            @NotBlank @Size(max = 32) String accountNo,
            @NotBlank @Size(max = 160) String name,
            @Size(max = 160) String fatherOrHusbandName,
            @Email @NotBlank @Size(max = 160) String email,
            @Size(max = 255) String address,
            @DecimalMin("0.00") BigDecimal compulsoryDepositAmount,
            @DecimalMin("0.00") BigDecimal maxCreditLimit) {}

    public record UpdateRequest(
            @NotBlank @Size(max = 160) String name,
            @Size(max = 160) String fatherOrHusbandName,
            @Email @NotBlank @Size(max = 160) String email,
            @Size(max = 255) String address,
            @DecimalMin("0.00") BigDecimal compulsoryDepositAmount,
            @DecimalMin("0.00") BigDecimal maxCreditLimit,
            Boolean active) {}

    public record Response(
            Long id,
            String accountNo,
            String name,
            String fatherOrHusbandName,
            String email,
            String address,
            Role role,
            boolean mustChangePassword,
            boolean active,
            BigDecimal compulsoryDepositAmount,
            BigDecimal maxCreditLimit) {

        public static Response of(Member m) {
            return new Response(m.getId(), m.getAccountNo(), m.getName(), m.getFatherOrHusbandName(),
                    m.getEmail(), m.getAddress(), m.getRole(), m.isMustChangePassword(), m.isActive(),
                    m.getCompulsoryDepositAmount(), m.getMaxCreditLimit());
        }
    }

    /** Returned once when an account is created so the admin can share the temp password. */
    public record CreatedResponse(Response member, String temporaryPassword) {}
}
