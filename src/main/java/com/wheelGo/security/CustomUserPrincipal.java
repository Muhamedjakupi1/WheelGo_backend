package com.wheelGo.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class CustomUserPrincipal implements UserDetails {

    private final UUID userId;
    private final String email;
    private final String password;
    private final String role;
    private final UUID tenantId;
    private final String tenantSlug;
    private final boolean impersonating;
    private final String originalRole;
    private final UUID originalUserId;

    public CustomUserPrincipal(UUID userId,
                               String email,
                               String password,
                               String role,
                               UUID tenantId,
                               String tenantSlug,
                               boolean impersonating,
                               String originalRole,
                               UUID originalUserId) {
        this.userId = userId;
        this.email = email;
        this.password = password;
        this.role = role;
        this.tenantId = tenantId;
        this.tenantSlug = tenantSlug;
        this.impersonating = impersonating;
        this.originalRole = originalRole;
        this.originalUserId = originalUserId;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getTenantSlug() {
        return tenantSlug;
    }

    public boolean isImpersonating() {
        return impersonating;
    }

    public String getOriginalRole() {
        return originalRole;
    }

    public UUID getOriginalUserId() {
        return originalUserId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}