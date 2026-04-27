package com.wheelGo.config;

import com.wheelGo.repository.TenantRepository;
import com.wheelGo.schema.TenantContext;
import com.wheelGo.security.CustomUserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class TenantSchemaFilter extends OncePerRequestFilter {

    private final TenantRepository tenantRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            resolveTenantSchema();
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private void resolveTenantSchema() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserPrincipal principal)) {
            return;
        }

        String tenantSlug = principal.getTenantSlug();
        if (tenantSlug == null || tenantSlug.isBlank()) {
            return;
        }

        tenantRepository.findBySlug(tenantSlug)
                .map(tenant -> tenant.getSchemaName())
                .filter(schemaName -> schemaName != null && !schemaName.isBlank())
                .ifPresent(TenantContext::setCurrentSchema);
    }
}
