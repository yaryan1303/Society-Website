package com.esicsociety.ams.security;

import com.esicsociety.ams.common.exception.ApiExceptions.ForbiddenException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Convenience accessors for the authenticated principal + ownership checks. */
public final class CurrentUser {

    private CurrentUser() {}

    public static AppUserPrincipal principal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUserPrincipal p)) {
            throw new ForbiddenException("Not authenticated");
        }
        return p;
    }

    public static Long memberId() {
        return principal().getMemberId();
    }

    public static boolean isAdmin() {
        return principal().isAdmin();
    }

    /**
     * Enforces that the caller is ADMIN or is the owner of {@code targetMemberId}.
     * This is the server-side guarantee that a member can never read another
     * member's data — enforced in the service layer, not just the UI.
     */
    public static void requireOwnerOrAdmin(Long targetMemberId) {
        AppUserPrincipal p = principal();
        if (!p.isAdmin() && !p.getMemberId().equals(targetMemberId)) {
            throw new ForbiddenException("You may only access your own account");
        }
    }
}
