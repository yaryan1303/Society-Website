package com.esicsociety.ams.auth;

import com.esicsociety.ams.auth.dto.AuthDtos;
import com.esicsociety.ams.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthDtos.LoginResponse login(@Valid @RequestBody AuthDtos.LoginRequest req) {
        return authService.login(req);
    }

    /**
     * First-login (forced) and voluntary password change. The request must carry
     * a valid JWT — the member authenticates with their temp password first.
     */
    @PostMapping("/change-password")
    public Map<String, String> changePassword(@Valid @RequestBody AuthDtos.ChangePasswordRequest req) {
        authService.changePassword(CurrentUser.memberId(), req);
        return Map.of("status", "ok");
    }

    @PostMapping("/forgot-password")
    public Map<String, String> forgotPassword(@Valid @RequestBody AuthDtos.ForgotPasswordRequest req) {
        authService.forgotPassword(req);
        // Always the same response, regardless of whether the account exists.
        return Map.of("status", "If the account exists, a reset link has been sent to its email.");
    }

    @PostMapping("/reset-password")
    public Map<String, String> resetPassword(@Valid @RequestBody AuthDtos.ResetPasswordRequest req) {
        authService.resetPassword(req);
        return Map.of("status", "ok");
    }
}
