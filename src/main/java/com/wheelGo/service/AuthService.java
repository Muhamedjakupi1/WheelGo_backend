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
import com.wheelGo.validation.PasswordPolicy;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.regex.Pattern;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@Service
@AllArgsConstructor
public class AuthService {
    private static final Pattern PHONE_ALLOWED_CHARS = Pattern.compile("^[0-9+()\\-\\s]+$");

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuditLogService auditLogService;
    private final UserProfileRepository userProfileRepository;
    private final TenantSchemaExecutor tenantSchemaExecutor;
    private final CacheInvalidationService cacheInvalidationService;
    private final UserSettingsService userSettingsService;


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
                JwtUtils.credentialVersion(user.getPasswordHash()),
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

        String email = required(req.email(), "Email").toLowerCase();
        String firstName = required(req.firstName(), "First name");
        String lastName = required(req.lastName(), "Last name");
        String phone = validatePhoneNumber(req.phone());

        Tenant tenant = tenantRepository.findBySlug(req.tenantSlug())
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        if (!tenant.isActive()) {
            throw new RuntimeException("Tenant is inactive");
        }

        if (userRepository.existsByEmailAndTenantId(email, tenant.getId())) {
            throw new RuntimeException("User already exists in this tenant");
        }

        if (!PasswordPolicy.isValid(req.password())) {
            throw new RuntimeException(PasswordPolicy.MESSAGE);
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(Role.USER);
        user.setTenant(tenant);
        user.setActive(true);
        user.setEmailVerified(true);

        User saved = userRepository.save(user);

        tenantSchemaExecutor.runForTenant(tenant, () -> {
            UserProfile profile = new UserProfile();
            profile.setUser(saved);
            profile.setFirstName(firstName);
            profile.setLastName(lastName);
            profile.setPhone(phone);
            userProfileRepository.save(profile);
            userSettingsService.createInitialSettings(saved);
        });

        cacheInvalidationService.evictUsersForTenant(tenant.getId());

        auditLogService.logForSchema(tenant.getSchemaName(), saved.getId(), AuditAction.CREATE, "User", saved.getId(), null, saved);

        CustomUserPrincipal principal = new CustomUserPrincipal(
                saved.getId(),
                saved.getEmail(),
                saved.getPasswordHash(),
                JwtUtils.credentialVersion(saved.getPasswordHash()),
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

    private String required(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException(fieldName + " is required");
        }
        return value.trim();
    }

    private String validatePhoneNumber(String value) {
        String phone = required(value, "Phone number");
        if (!PHONE_ALLOWED_CHARS.matcher(phone).matches()) {
            throw new ResponseStatusException(BAD_REQUEST, "Phone number can contain only digits, spaces, '+', '-', and parentheses");
        }

        long digitCount = phone.chars().filter(Character::isDigit).count();
        if (digitCount < 6 || digitCount > 15) {
            throw new ResponseStatusException(BAD_REQUEST, "Phone number must contain between 6 and 15 digits");
        }

        return phone;
    }
}
