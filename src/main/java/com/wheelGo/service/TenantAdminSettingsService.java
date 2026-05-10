package com.wheelGo.service;

import com.wheelGo.model.tenant.Tenant;
import com.wheelGo.model.tenant_settings.TenantSettingsResponse;
import com.wheelGo.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantAdminSettingsService {

    private final AdminAccessService adminAccessService;
    private final TenantRepository tenantRepository;
    private final TenantSettingsService tenantSettingsService;

    @Transactional(readOnly = true)
    public TenantSettingsResponse getCurrentTenantSettings() {
        UUID tenantId = adminAccessService.requireCurrentTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));

        return tenantSettingsService.getForTenant(tenant.getSchemaName());
    }
}
