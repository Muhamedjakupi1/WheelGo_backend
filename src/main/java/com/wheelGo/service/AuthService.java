package com.wheelGo.service;

import com.wheelGo.model.tenant.Tenant;
import com.wheelGo.model.user.User;
import com.wheelGo.model.enums.Role;
import com.wheelGo.repository.TenantRepository;
import com.wheelGo.repository.UserRepository;
import com.wheelGo.auth.AuthLoginRequest;
import com.wheelGo.auth.AuthResponse;
import com.wheelGo.auth.AuthSignUpRequest;
import com.wheelGo.security.CustomUserPrincipal;
import com.wheelGo.security.JwtUtils;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public AuthResponse login(AuthLoginRequest req) {
       Tenant tenant = tenantRepository.findBySlug(req.tenantSlug()).orElseThrow(
               () -> new RuntimeException("Tenant not found")
       );

        if (!tenant.isActive()) {
            throw new RuntimeException("Tenant account is inactive");
        }

        User user = userRepository.findByEmail(req.email().trim().toLowerCase()).orElseThrow(
                ()->new RuntimeException("Invalid credentials")
        );

        if(user.getTenant() == null || !user.getTenant().getId().equals(tenant.getId())) {
            throw new RuntimeException("Invalid credentials for this tenant");
        }

        if(!user.isActive()){
            throw new RuntimeException("User account is inactive");
        }

        if(!passwordEncoder.matches(req.password(), user.getPasswordHash())){
            throw new RuntimeException("Invalid credentials");
        }


       String tenantSlug = tenant.getSlug();
        var tenantId = tenant.getId();

        CustomUserPrincipal principal = new CustomUserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole().name(),
                tenantId,
                tenantSlug,
                false,
                null,
                null
        );

        String token = jwtUtils.generateToken(principal);

        return new AuthResponse(
                token,
                user.getEmail(),
                user.getRole().name(),
                user.getId(),
                tenantId,
                tenantSlug,
                false,
                null,
                null
        );
    }

    public AuthResponse signup(AuthSignUpRequest req) {
        Tenant tenant = tenantRepository.findBySlug(req.tenantSlug())
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        if (!tenant.isActive()) {
            throw new RuntimeException("Tenant is inactive");
        }

        if (userRepository.existsByEmail(req.email())) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setEmail(req.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(Role.USER);
        user.setTenant(tenant);
        user.setActive(true);
        user.setEmailVerified(true);

        User saved = userRepository.save(user);

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