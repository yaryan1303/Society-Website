package com.esicsociety.ams.security;

import com.esicsociety.ams.common.Role;
import com.esicsociety.ams.member.Member;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** Authenticated principal placed in the SecurityContext. */
public class AppUserPrincipal implements UserDetails {

    private final Long memberId;
    private final String accountNo;
    private final Role role;
    private final String passwordHash;
    private final boolean active;
    private final boolean mustChangePassword;

    public AppUserPrincipal(Long memberId, String accountNo, Role role, String passwordHash,
                            boolean active, boolean mustChangePassword) {
        this.memberId = memberId;
        this.accountNo = accountNo;
        this.role = role;
        this.passwordHash = passwordHash;
        this.active = active;
        this.mustChangePassword = mustChangePassword;
    }

    public static AppUserPrincipal from(Member m) {
        return new AppUserPrincipal(m.getId(), m.getAccountNo(), m.getRole(),
                m.getPasswordHash(), m.isActive(), m.isMustChangePassword());
    }

    public Long getMemberId() { return memberId; }
    public Role getRole() { return role; }
    public boolean isAdmin() { return role == Role.ADMIN; }
    public boolean isMustChangePassword() { return mustChangePassword; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return accountNo; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return active; }
}
