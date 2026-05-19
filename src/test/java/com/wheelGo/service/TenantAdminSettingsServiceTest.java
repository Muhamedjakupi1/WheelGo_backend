package com.wheelGo.service;

import com.wheelGo.model.tenant.Tenant;
import com.wheelGo.model.tenant_settings.TenantSettingsResponse;
import com.wheelGo.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantAdminSettingsServiceTest {

    @Mock private AdminAccessService adminAccessService;
    @Mock private TenantRepository tenantRepository;
    @Mock private TenantSettingsService tenantSettingsService;
    @InjectMocks private TenantAdminSettingsService tenantAdminSettingsService;

    @Test
    void should_return_current_tenant_settings_when_tenant_exists() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setSchemaName("tenant_one");
        TenantSettingsResponse response = new TenantSettingsResponse();
        response.setCurrency("EUR");

        when(adminAccessService.requireCurrentTenantId()).thenReturn(tenantId);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(tenantSettingsService.getForTenant("tenant_one")).thenReturn(response);

        TenantSettingsResponse result = tenantAdminSettingsService.getCurrentTenantSettings();

        assertThat(result.getCurrency()).isEqualTo("EUR");
    }

    @Test
    void should_throw_not_found_when_current_tenant_missing() {
        UUID tenantId = UUID.randomUUID();
        when(adminAccessService.requireCurrentTenantId()).thenReturn(tenantId);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantAdminSettingsService.getCurrentTenantSettings())
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Tenant not found");
    }
}
