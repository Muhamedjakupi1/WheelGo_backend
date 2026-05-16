package com.wheelGo.controller;

import com.wheelGo.model.tenant_settings.TenantSettingsResponse;
import com.wheelGo.service.TenantAdminSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenant-settings")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')")
public class TenantSettingsController {

    private final TenantAdminSettingsService tenantAdminSettingsService;

    @GetMapping
    public ResponseEntity<TenantSettingsResponse> getCurrentTenantSettings() {
        return ResponseEntity.ok(tenantAdminSettingsService.getCurrentTenantSettings());
    }
}
