package com.wheelGo.service;

import com.wheelGo.auth.AuthLoginRequest;
import com.wheelGo.auth.AuthResponse;
import com.wheelGo.auth.AuthSignUpRequest;
import com.wheelGo.model.enums.Role;
import com.wheelGo.model.tenant.Tenant;
import com.wheelGo.model.user.User;
import com.wheelGo.repository.TenantRepository;
import com.wheelGo.repository.UserProfileRepository;
import com.wheelGo.repository.UserRepository;
import com.wheelGo.schema.TenantSchemaExecutor;
import com.wheelGo.security.JwtUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtils jwtUtils;
    @Mock private AuditLogService auditLogService;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private TenantSchemaExecutor tenantSchemaExecutor;
    @InjectMocks private AuthService authService;

    @Test
    void should_return_auth_response_when_login_credentials_valid() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setSlug("tenant");
        tenant.setSchemaName("tenant_schema");
        tenant.setActive(true);
        User user = new User();
        user.setId(userId);
        user.setEmail("user@example.com");
        user.setPasswordHash("hash");
        user.setRole(Role.USER);
        user.setActive(true);

        when(tenantRepository.findBySlug("tenant")).thenReturn(Optional.of(tenant));
        when(userRepository.findByEmailAndTenantId("user@example.com", tenantId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password1", "hash")).thenReturn(true);
        when(jwtUtils.generateToken(any())).thenReturn("token");

        AuthResponse result = authService.login(new AuthLoginRequest("user@example.com", "Password1", "tenant"));

        assertThat(result.token()).isEqualTo("token");
        assertThat(result.email()).isEqualTo("user@example.com");
    }

    @Test
    void should_throw_runtime_exception_when_login_password_invalid() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setActive(true);
        User user = new User();
        user.setEmail("user@example.com");
        user.setPasswordHash("hash");
        user.setActive(true);
        user.setRole(Role.USER);

        when(tenantRepository.findBySlug("tenant")).thenReturn(Optional.of(tenant));
        when(userRepository.findByEmailAndTenantId("user@example.com", tenantId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new AuthLoginRequest("user@example.com", "wrong", "tenant")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    void should_signup_user_when_request_valid() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setSlug("tenant");
        tenant.setSchemaName("tenant_schema");
        tenant.setActive(true);
        User saved = new User();
        saved.setId(userId);
        saved.setEmail("user@example.com");
        saved.setPasswordHash("encoded");
        saved.setRole(Role.USER);

        when(tenantRepository.findBySlug("tenant")).thenReturn(Optional.of(tenant));
        when(userRepository.existsByEmailAndTenantId("user@example.com", tenantId)).thenReturn(false);
        when(passwordEncoder.encode("Password1")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(1);
            runnable.run();
            return null;
        }).when(tenantSchemaExecutor).runForTenant(any(Tenant.class), any(Runnable.class));
        when(jwtUtils.generateToken(any())).thenReturn("token");

        AuthResponse result = authService.signup(
                new AuthSignUpRequest("user@example.com", "Password1", "tenant", "John", "Doe", "123")
        );

        assertThat(result.token()).isEqualTo("token");
        verify(userProfileRepository).save(any());
    }

    @Test
    void should_throw_forbidden_when_signup_slug_reserved() {
        assertThatThrownBy(() -> authService.signup(
                new AuthSignUpRequest("user@example.com", "Password1", "super-admin-tenant", "John", "Doe", "123")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Public signup is disabled for this tenant");
    }
}
