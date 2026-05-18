package com.wheelGo.controller;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImpersonationControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private ImpersonationController impersonationController;

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

        ResponseEntity<?> response = impersonationController.start("tenant-one", superAdmin);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasFieldOrPropertyWithValue("token", "impersonation-token");
        assertThat(response.getBody()).hasFieldOrPropertyWithValue("role", "ADMIN");
        assertThat(response.getBody()).hasFieldOrPropertyWithValue("isImpersonating", true);
        assertThat(response.getBody()).hasFieldOrPropertyWithValue("tenantSlug", "tenant-one");
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

        assertThatThrownBy(() -> impersonationController.start("tenant-one", superAdmin))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
