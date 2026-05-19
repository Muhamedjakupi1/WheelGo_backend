package com.wheelGo.service;

import com.wheelGo.auth.AuthResponse;
import com.wheelGo.model.enums.Role;
import com.wheelGo.model.tenant.Tenant;
import com.wheelGo.model.user.User;
import com.wheelGo.repository.TenantRepository;
import com.wheelGo.repository.UserRepository;
import com.wheelGo.security.CustomUserPrincipal;
import com.wheelGo.security.JwtUtils;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImpersonationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private ImpersonationService impersonationService;

    @Test
    void should_start_impersonation_for_first_active_tenant_admin() {
        UUID tenantId = UUID.randomUUID();
        UUID superAdminId = UUID.randomUUID();
        UUID tenantAdminId = UUID.randomUUID();

        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setSlug("tenant-one");
        tenant.setActive(true);

        User tenantAdmin = new User();
        tenantAdmin.setId(tenantAdminId);
        tenantAdmin.setEmail("admin@tenant.com");
        tenantAdmin.setPasswordHash("hash");
        tenantAdmin.setRole(Role.ADMIN);
        tenantAdmin.setActive(true);

        CustomUserPrincipal superAdmin = new CustomUserPrincipal(
                superAdminId,
                "super@wheelgo.com",
                "hash",
                "cred",
                Role.SUPER_ADMIN.name(),
                UUID.randomUUID(),
                "super-admin-tenant",
                false,
                null,
                null
        );

        when(tenantRepository.findBySlug("tenant-one")).thenReturn(Optional.of(tenant));
        when(userRepository.findFirstByTenantIdAndRoleAndIsActiveTrueOrderByCreatedAtAsc(tenantId, Role.ADMIN))
                .thenReturn(Optional.of(tenantAdmin));
        when(jwtUtils.generateImpersonationToken(any(), any(), any())).thenReturn("impersonation-token");

        AuthResponse response = impersonationService.startForTenant("tenant-one", superAdmin);

        assertThat(response.token()).isEqualTo("impersonation-token");
        assertThat(response.role()).isEqualTo("ADMIN");
        assertThat(response.isImpersonating()).isTrue();
        assertThat(response.tenantSlug()).isEqualTo("tenant-one");
        assertThat(response.originalRole()).isEqualTo("SUPER_ADMIN");
        assertThat(response.originalUserId()).isEqualTo(superAdminId);
    }

    @Test
    void should_reject_impersonation_when_tenant_is_inactive() {
        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setSlug("tenant-one");
        tenant.setActive(false);

        CustomUserPrincipal superAdmin = new CustomUserPrincipal(
                UUID.randomUUID(),
                "super@wheelgo.com",
                "hash",
                "cred",
                Role.SUPER_ADMIN.name(),
                UUID.randomUUID(),
                "super-admin-tenant",
                false,
                null,
                null
        );

        when(tenantRepository.findBySlug("tenant-one")).thenReturn(Optional.of(tenant));

        assertThatThrownBy(() -> impersonationService.startForTenant("tenant-one", superAdmin))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Tenant account is inactive");
    }

    @Test
    void should_stop_impersonation_and_restore_original_super_admin() {
        UUID originalUserId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setSlug("super-admin-tenant");

        User superAdmin = new User();
        superAdmin.setId(originalUserId);
        superAdmin.setEmail("super@wheelgo.com");
        superAdmin.setPasswordHash("hash");
        superAdmin.setRole(Role.SUPER_ADMIN);
        superAdmin.setTenant(tenant);

        CustomUserPrincipal impersonated = new CustomUserPrincipal(
                UUID.randomUUID(),
                "admin@tenant.com",
                "hash",
                "cred",
                Role.ADMIN.name(),
                UUID.randomUUID(),
                "tenant-one",
                true,
                Role.SUPER_ADMIN.name(),
                originalUserId
        );

        when(userRepository.findById(originalUserId)).thenReturn(Optional.of(superAdmin));
        when(jwtUtils.generateToken(any())).thenReturn("restored-token");

        AuthResponse response = impersonationService.stop(impersonated);

        assertThat(response.token()).isEqualTo("restored-token");
        assertThat(response.role()).isEqualTo("SUPER_ADMIN");
        assertThat(response.isImpersonating()).isFalse();
        assertThat(response.originalRole()).isNull();
        assertThat(response.originalUserId()).isNull();
    }
}
