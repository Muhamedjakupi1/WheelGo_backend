package com.wheelGo.security;

import com.wheelGo.model.tenant.Tenant;
import com.wheelGo.model.user.User;
import com.wheelGo.repository.TenantRepository;
import com.wheelGo.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SwaggerDevAuthenticationFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-Slug";

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;

    @Value("${app.swagger.dev-auth-bypass.enabled:true}")
    private boolean swaggerDevAuthBypassEnabled;

    @Value("${app.swagger.dev-auth-bypass.email:admin_wheelgo@gmail.com}")
    private String swaggerDevAuthEmail;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (shouldBypass(request)) {
            resolvePrincipal(request).ifPresent(principal -> {
                var authentication = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldBypass(HttpServletRequest request) {
        if (!swaggerDevAuthBypassEnabled) {
            return false;
        }
        if (!request.getRequestURI().startsWith("/api/")) {
            return false;
        }
        if (request.getRequestURI().startsWith("/api/auth/")
                || request.getRequestURI().startsWith("/api/public/")) {
            return false;
        }
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return false;
        }
        if (hasBearerToken(request)) {
            return false;
        }
        return true;
    }

    private boolean hasBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        return authHeader != null && authHeader.startsWith("Bearer ");
    }

    private Optional<CustomUserPrincipal> resolvePrincipal(HttpServletRequest request) {
        if (request.getRequestURI().startsWith("/api/super-admin/")) {
            return findConfiguredDevUser()
                    .or(() -> findAnyActiveUser(null));
        }

        String tenantSlug = trimToNull(request.getHeader(TENANT_HEADER));
        if (tenantSlug != null) {
            return tenantRepository.findBySlug(tenantSlug)
                    .flatMap(tenant -> findTenantAdmin(tenant)
                            .or(() -> findAnyTenantUser(tenant))
                            .or(() -> findConfiguredDevUser())
                            .or(() -> findAnyActiveUser(tenant)));
        }

        return findConfiguredDevUser()
                .or(() -> findAnyActiveUser(null));
    }

    private Optional<CustomUserPrincipal> findConfiguredDevUser() {
        return userRepository.findByEmail(swaggerDevAuthEmail.trim().toLowerCase())
                .map(this::toPrincipal);
    }

    private Optional<CustomUserPrincipal> findTenantAdmin(Tenant tenant) {
        return userRepository.findFirstByTenantIdAndRoleAndIsActiveTrueOrderByCreatedAtAsc(tenant.getId(), com.wheelGo.model.enums.Role.ADMIN)
                .map(user -> toSwaggerPrincipal(user, tenant));
    }

    private Optional<CustomUserPrincipal> findAnyTenantUser(Tenant tenant) {
        return userRepository.findFirstByTenantIdAndIsActiveTrueOrderByCreatedAtAsc(tenant.getId())
                .map(user -> toSwaggerPrincipal(user, tenant));
    }

    private Optional<CustomUserPrincipal> findAnyActiveUser(Tenant requestedTenant) {
        return userRepository.findFirstByIsActiveTrueOrderByCreatedAtAsc()
                .map(user -> toSwaggerPrincipal(user, requestedTenant));
    }

    private CustomUserPrincipal toPrincipal(User user) {
        return toSwaggerPrincipal(user, user.getTenant());
    }

    private CustomUserPrincipal toSwaggerPrincipal(User user, Tenant requestedTenant) {
        Tenant tenant = requestedTenant != null ? requestedTenant : user.getTenant();
        return new CustomUserPrincipal(
                user.getId(),
                user.getEmail(),
                null,
                com.wheelGo.model.enums.Role.SUPER_ADMIN.name(),
                tenant != null ? tenant.getId() : null,
                tenant != null ? tenant.getSlug() : null,
                false,
                null,
                null
        );
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
