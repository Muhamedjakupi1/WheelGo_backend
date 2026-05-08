package com.wheelGo.service;

import com.wheelGo.security.CustomUserPrincipal;
import com.wheelGo.tools.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class AdminAccessService {

    public UUID requireCurrentTenantId() {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
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
}
