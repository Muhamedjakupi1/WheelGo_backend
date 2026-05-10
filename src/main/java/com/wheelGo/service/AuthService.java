package com.wheelGo.service;

import com.wheelGo.auth.AuthLoginRequest;
import com.wheelGo.auth.AuthResponse;
import com.wheelGo.auth.AuthSignUpRequest;
import com.wheelGo.model.enums.AuditAction;
import com.wheelGo.model.enums.Role;
import com.wheelGo.model.tenant.Tenant;
import com.wheelGo.model.user.User;
import com.wheelGo.model.user_profiles.UserProfile;
import com.wheelGo.repository.TenantRepository;
import com.wheelGo.repository.UserProfileRepository;
import com.wheelGo.repository.UserRepository;
import com.wheelGo.schema.TenantSchemaExecutor;
import com.wheelGo.security.CustomUserPrincipal;
import com.wheelGo.security.JwtUtils;
import com.wheelGo.security.ReservedTenantSlugs;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@Service
@AllArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuditLogService auditLogService;
    private final UserProfileRepository userProfileRepository;
    private final TenantSchemaExecutor tenantSchemaExecutor;


    public AuthResponse login(AuthLoginRequest req) {

        Tenant tenant = tenantRepository.findBySlug(req.tenantSlug())
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        if (!tenant.isActive()) {
            throw new RuntimeException("Tenant account is inactive");
        }

        User user = userRepository
                .findByEmailAndTenantId(req.email().trim().toLowerCase(), tenant.getId())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!user.isActive()) {
            throw new RuntimeException("User account is inactive");
        }

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        CustomUserPrincipal principal = new CustomUserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole().name(),
                tenant.getId(),
                tenant.getSlug(),
                false,
                null,
                null
        );

        String token = jwtUtils.generateToken(principal);

        auditLogService.logForSchema(tenant.getSchemaName(), user.getId(), AuditAction.LOGIN, "User", user.getId(), null, user);

        return new AuthResponse(
                token,
                user.getEmail(),
                user.getRole().name(),
                user.getId(),
                tenant.getId(),
                tenant.getSlug(),
                false,
                null,
                null
        );
    }

    public AuthResponse signup(AuthSignUpRequest req) {
        if (ReservedTenantSlugs.isReserved(req.tenantSlug())) {
            throw new ResponseStatusException(FORBIDDEN, "Public signup is disabled for this tenant");
        }

        Tenant tenant = tenantRepository.findBySlug(req.tenantSlug())
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        if (!tenant.isActive()) {
            throw new RuntimeException("Tenant is inactive");
        }

        if (userRepository.existsByEmailAndTenantId(req.email().trim().toLowerCase(), tenant.getId())) {
            throw new RuntimeException("User already exists in this tenant");
        }

        User user = new User();
        user.setEmail(req.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(Role.USER);
        user.setTenant(tenant);
        user.setActive(true);
        user.setEmailVerified(true);

        User saved = userRepository.save(user);

        tenantSchemaExecutor.runForTenant(tenant, () -> {
            UserProfile profile = new UserProfile();
            profile.setUser(saved);
            profile.setFirstName("User");
            profile.setLastName("User");
            userProfileRepository.save(profile);
        });

        auditLogService.logForSchema(tenant.getSchemaName(), saved.getId(), AuditAction.CREATE, "User", saved.getId(), null, saved);

        CustomUserPrincipal principal = new CustomUserPrincipal(
                saved.getId(),
                saved.getEmail(),
                saved.getPasswordHash(),
                saved.getRole().name(),
                tenant.getId(),
                tenant.getSlug(),
                false,
                null,
                null
        );

        String token = jwtUtils.generateToken(principal);

        return new AuthResponse(
                token,
                saved.getEmail(),
                saved.getRole().name(),
                saved.getId(),
                tenant.getId(),
                tenant.getSlug(),
                false,
                null,
                null
        );
    }
}
