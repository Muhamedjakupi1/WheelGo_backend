package com.wheelGo.service;

import com.wheelGo.model.enums.Role;
import com.wheelGo.model.tenant.Tenant;
import com.wheelGo.model.user.User;
import com.wheelGo.model.user.UserResponse;
import com.wheelGo.model.user.UserUpdateRequest;
import com.wheelGo.repository.UserRepository;
import com.wheelGo.security.CustomUserPrincipal;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AdminAccessService adminAccessService;
    @Mock private EntityManager entityManager;
    @Mock private Query query;

    @InjectMocks
    private UserAdminService userAdminService;

    private UUID tenantId;
    private UUID userId;
    private User user;
    private CustomUserPrincipal adminPrincipal;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);

        user = new User();
        user.setId(userId);
        user.setEmail("user@example.com");
        user.setRole(Role.USER);
        user.setTenant(tenant);
        user.setActive(true);
        user.setEmailVerified(true);

        adminPrincipal = new CustomUserPrincipal(
                UUID.randomUUID(), "admin@example.com", "hash", "v1", "ADMIN",
                tenantId, "tenant", false, null, null
        );
    }

    @Test
    void should_return_all_users_when_get_all() {
        when(adminAccessService.requireCurrentTenantId()).thenReturn(tenantId);
        when(userRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId)).thenReturn(List.of(user));

        List<UserResponse> result = userAdminService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void should_return_user_when_get_by_id_found() {
        when(adminAccessService.requireCurrentTenantId()).thenReturn(tenantId);
        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(user));

        UserResponse result = userAdminService.getById(userId);

        assertThat(result.getId()).isEqualTo(userId);
    }

    @Test
    void should_throw_not_found_when_get_by_id_missing() {
        when(adminAccessService.requireCurrentTenantId()).thenReturn(tenantId);
        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userAdminService.getById(userId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void should_update_user_when_request_valid() {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setEmail(" New@Example.com ");
        request.setPassword("Password1");
        request.setRole(Role.ADMIN);
        request.setIsActive(false);

        when(adminAccessService.requireCurrentTenantId()).thenReturn(tenantId);
        when(adminAccessService.requireCurrentPrincipal()).thenReturn(adminPrincipal);
        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndTenantIdAndIdNot("new@example.com", tenantId, userId)).thenReturn(false);
        when(passwordEncoder.encode("Password1")).thenReturn("encoded");
        when(userRepository.save(user)).thenReturn(user);

        UserResponse result = userAdminService.update(userId, request);

        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getPasswordHash()).isEqualTo("encoded");
        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
        assertThat(user.isActive()).isFalse();
        assertThat(result.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void should_throw_bad_request_when_email_already_exists() {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setEmail("dup@example.com");

        when(adminAccessService.requireCurrentTenantId()).thenReturn(tenantId);
        when(adminAccessService.requireCurrentPrincipal()).thenReturn(adminPrincipal);
        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndTenantIdAndIdNot("dup@example.com", tenantId, userId)).thenReturn(true);

        assertThatThrownBy(() -> userAdminService.update(userId, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("A user with this email already exists");
    }

    @Test
    void should_throw_bad_request_when_password_invalid() {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setPassword("weak");

        when(adminAccessService.requireCurrentTenantId()).thenReturn(tenantId);
        when(adminAccessService.requireCurrentPrincipal()).thenReturn(adminPrincipal);
        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userAdminService.update(userId, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Password must");
    }

    @Test
    void should_throw_bad_request_when_deactivating_own_account() {
        CustomUserPrincipal self = new CustomUserPrincipal(
                userId, "user@example.com", "hash", "v1", "ADMIN",
                tenantId, "tenant", false, null, null
        );
        user.setRole(Role.ADMIN);
        UserUpdateRequest request = new UserUpdateRequest();
        request.setIsActive(false);

        when(adminAccessService.requireCurrentTenantId()).thenReturn(tenantId);
        when(adminAccessService.requireCurrentPrincipal()).thenReturn(self);
        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userAdminService.update(userId, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("cannot deactivate your own account");
    }

    @Test
    void should_throw_bad_request_when_demoting_own_admin_account() {
        CustomUserPrincipal self = new CustomUserPrincipal(
                userId, "user@example.com", "hash", "v1", "ADMIN",
                tenantId, "tenant", false, null, null
        );
        user.setRole(Role.ADMIN);
        UserUpdateRequest request = new UserUpdateRequest();
        request.setRole(Role.USER);

        when(adminAccessService.requireCurrentTenantId()).thenReturn(tenantId);
        when(adminAccessService.requireCurrentPrincipal()).thenReturn(self);
        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userAdminService.update(userId, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("cannot demote your own admin account");
    }

    @Test
    void should_throw_forbidden_when_non_super_admin_assigns_super_admin_role() {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setRole(Role.SUPER_ADMIN);

        when(adminAccessService.requireCurrentTenantId()).thenReturn(tenantId);
        when(adminAccessService.requireCurrentPrincipal()).thenReturn(adminPrincipal);
        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userAdminService.update(userId, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only super admins can assign the SUPER_ADMIN role");
    }

    @Test
    void should_delete_user_when_not_self() {
        when(adminAccessService.requireCurrentTenantId()).thenReturn(tenantId);
        when(adminAccessService.requireCurrentPrincipal()).thenReturn(adminPrincipal);
        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(user));
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter("userId", userId)).thenReturn(query);
        when(query.executeUpdate()).thenReturn(1);

        userAdminService.delete(userId);

        verify(userRepository).delete(user);
    }

    @Test
    void should_throw_bad_request_when_deleting_own_account() {
        CustomUserPrincipal self = new CustomUserPrincipal(
                userId, "user@example.com", "hash", "v1", "ADMIN",
                tenantId, "tenant", false, null, null
        );

        when(adminAccessService.requireCurrentTenantId()).thenReturn(tenantId);
        when(adminAccessService.requireCurrentPrincipal()).thenReturn(self);
        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userAdminService.delete(userId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("cannot delete your own account");

        verify(userRepository, never()).delete(user);
    }
}
