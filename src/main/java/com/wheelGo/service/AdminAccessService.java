package com.wheelGo.service;

import com.wheelGo.repository.TenantRepository;
import com.wheelGo.security.CustomUserPrincipal;
import com.wheelGo.tools.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminAccessService {

    private final TenantRepository tenantRepository;
    private final HttpServletRequest request;

    public UUID requireCurrentTenantId() {
        CustomUserPrincipal principal = requireCurrentPrincipal();
        UUID tenantId = resolveTenantId(principal);
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated tenant context is missing");
        }
        return tenantId;
    }

    public CustomUserPrincipal requireCurrentPrincipal() {
        CustomUserPrincipal principal = SecurityUtils.getCurrentPrincipal();
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user context is missing");
        }
        return principal;
    }

    private UUID resolveTenantId(CustomUserPrincipal principal) {
        if ("SUPER_ADMIN".equals(principal.getRole())) {
            String requestedTenantSlug = request.getHeader("X-Tenant-Slug");
            if (requestedTenantSlug != null && !requestedTenantSlug.isBlank()) {
                return tenantRepository.findBySlug(requestedTenantSlug.trim())
                        .map(tenant -> tenant.getId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target tenant not found"));
            }
        }

        return principal.getTenantId();
    }
}
