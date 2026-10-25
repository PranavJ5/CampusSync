package com.campussync.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Objects;

public final class CampusUserPrincipal implements UserDetails {

    private final Long userId;
    private final String username;
    private final String email;
    private final String passwordHash;
    private final boolean active;
    private final String roleCode;

    public CampusUserPrincipal(Long userId, String username, String email, String passwordHash,
                               boolean active, String roleCode) {
        this.userId = Objects.requireNonNull(userId);
        this.username = Objects.requireNonNull(username);
        this.email = Objects.requireNonNull(email);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.active = active;
        this.roleCode = ApprovedRole.fromExactCode(roleCode)
                .orElseThrow(() -> new IllegalArgumentException("Unrecognized role code"))
                .name();
    }

    public Long getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getRoleCode() { return roleCode; }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return java.util.List.of(new SimpleGrantedAuthority("ROLE_" + roleCode));
    }
    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return username; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return active; }
}
