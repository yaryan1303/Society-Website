package com.esicsociety.ams.auth.dto;

import com.esicsociety.ams.common.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request/response DTOs for authentication. */
public final class AuthDtos {

    private AuthDtos() {}

    public record LoginRequest(
            @NotBlank String accountNo,
            @NotBlank String password) {}

    public record LoginResponse(
            String token,
            String tokenType,
            Long memberId,
            String accountNo,
            String name,
            Role role,
            boolean mustChangePassword,
            long expiresInMinutes) {}

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 72, message = "Password must be 8-72 characters") String newPassword) {}

    public record ForgotPasswordRequest(
            @NotBlank String accountNo) {}

    public record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 8, max = 72, message = "Password must be 8-72 characters") String newPassword) {}
}
