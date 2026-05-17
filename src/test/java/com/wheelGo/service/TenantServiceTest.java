package com.wheelGo.service;

import com.wheelGo.mapper.TenantMapper;
import com.wheelGo.model.enums.Plan;
import com.wheelGo.model.tenant.Tenant;
import com.wheelGo.model.tenant.TenantRequest;
import com.wheelGo.model.tenant.TenantResponse;
import com.wheelGo.model.tenant.TenantUpdateRequest;
import com.wheelGo.model.tenant_settings.TenantSettingsRequest;
import com.wheelGo.model.tenant_settings.TenantSettingsResponse;
import com.wheelGo.repository.TenantRepository;
import com.wheelGo.repository.UserRepository;
import com.wheelGo.schema.TenantSchemaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private TenantSchemaService schemaService;
    @Mock private TenantMapper tenantMapper;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditLogService auditLogService;
    @Mock private TenantSettingsService tenantSettingsService;
    @InjectMocks private TenantService tenantService;

    @Test
    void should_create_tenant_when_request_valid() {
        TenantRequest request = new TenantRequest();
        request.setName("Tenant One");
        request.setSlug("Tenant One");
        request.setPlan(Plan.FREE);
        request.setAdminEmail("admin@example.com");
        request.setAdminPassword("Password1");
        request.setSettings(new TenantSettingsRequest());

        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setSlug("tenant-one");
        tenant.setSchemaName("tenant_one");
        tenant.setName("Tenant One");
        tenant.setPlan(Plan.FREE);

        TenantResponse mapped = new TenantResponse();
        mapped.setId(tenant.getId());
        mapped.setSlug("tenant-one");
        TenantSettingsResponse settings = new TenantSettingsResponse();
        settings.setCurrency("EUR");

        when(tenantRepository.existsBySlug("tenant-one")).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);
        when(tenantSettingsService.createForTenant("tenant_one", request.getSettings())).thenReturn(settings);
        when(passwordEncoder.encode("Password1")).thenReturn("encoded");
        when(tenantMapper.toResponse(tenant)).thenReturn(mapped);

        TenantResponse result = tenantService.createTenant(request);

        assertThat(result.getSlug()).isEqualTo("tenant-one");
        verify(schemaService).createSchemaForTenant("tenant_one");
        verify(userRepository).save(any());
    }

    @Test
    void should_throw_bad_request_when_tenant_slug_exists() {
        TenantRequest request = new TenantRequest();
        request.setSlug("tenant");
        when(tenantRepository.existsBySlug("tenant")).thenReturn(true);

        assertThatThrownBy(() -> tenantService.createTenant(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void should_throw_bad_request_when_admin_password_invalid() {
        TenantRequest request = new TenantRequest();
        request.setSlug("tenant");
        request.setAdminPassword("weak");
        when(tenantRepository.existsBySlug("tenant")).thenReturn(false);

        assertThatThrownBy(() -> tenantService.createTenant(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Password must");
    }

    @Test
    void should_return_all_tenants_when_get_all() {
        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setSchemaName("tenant_one");
        TenantResponse response = new TenantResponse();
        response.setId(tenant.getId());

        when(tenantRepository.findAll()).thenReturn(List.of(tenant));
        when(tenantSettingsService.getForTenant("tenant_one")).thenReturn(new TenantSettingsResponse());
        when(tenantMapper.toResponse(tenant)).thenReturn(response);

        List<TenantResponse> result = tenantService.getAll();

        assertThat(result).hasSize(1);
    }

    @Test
    void should_delete_tenant_when_found() {
        UUID id = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(id);
        tenant.setSlug("tenant");
        tenant.setSchemaName("tenant_one");

        when(tenantRepository.findById(id)).thenReturn(Optional.of(tenant));

        tenantService.deleteTenant(id);

        verify(schemaService).dropSchemaForTenant("tenant_one");
        verify(tenantRepository).delete(tenant);
    }

    @Test
    void should_update_tenant_when_request_valid() {
        UUID id = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(id);
        tenant.setName("Old");
        tenant.setSlug("tenant");
        tenant.setSchemaName("tenant_one");
        tenant.setPlan(Plan.FREE);
        TenantUpdateRequest request = new TenantUpdateRequest();
        request.setName("New");
        request.setIsActive(false);

        TenantResponse mapped = new TenantResponse();
        mapped.setId(id);
        mapped.setName("New");

        when(tenantRepository.findById(id)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(tenant)).thenReturn(tenant);
        when(tenantSettingsService.getForTenant("tenant_one")).thenReturn(new TenantSettingsResponse());
        when(tenantMapper.toResponse(tenant)).thenReturn(mapped);

        TenantResponse result = tenantService.updateTenant(id, request);

        assertThat(tenant.getName()).isEqualTo("New");
        assertThat(tenant.isActive()).isFalse();
        assertThat(result.getName()).isEqualTo("New");
    }
}
