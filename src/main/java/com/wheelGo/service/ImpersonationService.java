package com.wheelGo.service;

import com.wheelGo.auth.AuthResponse;
import com.wheelGo.model.enums.Role;
import com.wheelGo.model.user.User;
import com.wheelGo.repository.TenantRepository;
import com.wheelGo.repository.UserRepository;
import com.wheelGo.security.CustomUserPrincipal;
import com.wheelGo.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImpersonationService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final JwtUtils jwtUtils;

    public AuthResponse startForTenant(String tenantSlug, CustomUserPrincipal superAdmin) {
        var tenant = tenantRepository.findBySlug(tenantSlug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));

        if (!tenant.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant account is inactive");
        }

        User target = userRepository.findFirstByTenantIdAndRoleAndIsActiveTrueOrderByCreatedAtAsc(tenant.getId(), Role.ADMIN)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active tenant admin found"));

        CustomUserPrincipal targetPrincipal = new CustomUserPrincipal(
                target.getId(),
                target.getEmail(),
                target.getPasswordHash(),
                JwtUtils.credentialVersion(target.getPasswordHash()),
                target.getRole().name(),
                tenant.getId(),
                tenant.getSlug(),
                true,
                superAdmin.getRole(),
                superAdmin.getUserId()
        );

        String token = jwtUtils.generateImpersonationToken(
                targetPrincipal,
                superAdmin.getUserId(),
                superAdmin.getRole()
        );

        return authResponse(
                token,
                target,
                tenant.getId(),
                tenant.getSlug(),
                true,
                superAdmin.getRole(),
                superAdmin.getUserId()
        );
    }

    public AuthResponse stop(CustomUserPrincipal current) {
        if (!current.isImpersonating()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not currently impersonating");
        }

        User superAdmin = userRepository.findById(current.getOriginalUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Original admin not found"));

        String slug = superAdmin.getTenant() != null ? superAdmin.getTenant().getSlug() : null;
        UUID tenantId = superAdmin.getTenant() != null ? superAdmin.getTenant().getId() : null;

        CustomUserPrincipal original = new CustomUserPrincipal(
                superAdmin.getId(),
                superAdmin.getEmail(),
                superAdmin.getPasswordHash(),
                JwtUtils.credentialVersion(superAdmin.getPasswordHash()),
                superAdmin.getRole().name(),
                tenantId,
                slug,
                false,
                null,
                null
        );

        String token = jwtUtils.generateToken(original);

        return authResponse(
                token,
                superAdmin,
                tenantId,
                slug,
                false,
                null,
                null
        );
    }

    private AuthResponse authResponse(String token,
                                      User user,
                                      UUID tenantId,
                                      String tenantSlug,
                                      boolean isImpersonating,
                                      String originalRole,
                                      UUID originalUserId) {
        return new AuthResponse(
                token,
                user.getEmail(),
                user.getRole().name(),
                user.getId(),
                tenantId,
                tenantSlug,
                isImpersonating,
                originalRole,
                originalUserId
        );
    }
}
