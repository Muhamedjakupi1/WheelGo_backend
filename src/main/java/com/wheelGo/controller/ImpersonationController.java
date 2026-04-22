package com.wheelGo.controller;

import com.wheelGo.model.user.User;
import com.wheelGo.repository.TenantRepository;
import com.wheelGo.repository.UserRepository;
import com.wheelGo.auth.AuthResponse;
import com.wheelGo.security.CustomUserPrincipal;
import com.wheelGo.security.JwtUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/super-admin/impersonation")
@CrossOrigin(origins = "http://localhost:5173")
public class ImpersonationController {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final JwtUtils jwtUtils;

    public ImpersonationController(UserRepository userRepository,
                                   TenantRepository tenantRepository,
                                   JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/start/{tenantSlug}/{targetUserId}")
    public ResponseEntity<?> start(@PathVariable String tenantSlug,
                                   @PathVariable UUID targetUserId,
                                   @org.springframework.security.core.annotation.AuthenticationPrincipal CustomUserPrincipal superAdmin) {

        var tenant = tenantRepository.findBySlug(tenantSlug)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        User target = userRepository.findByIdAndTenantId(targetUserId, tenant.getId())
                .orElseThrow(() -> new RuntimeException("User not found in this tenant"));

        CustomUserPrincipal targetPrincipal = new CustomUserPrincipal(
                target.getId(),
                target.getEmail(),
                null,
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

        return ResponseEntity.ok(new AuthResponse(
                token,
                target.getEmail(),
                target.getRole().name(),
                target.getId(),
                tenant.getId(),
                tenant.getSlug(),
                true,
                superAdmin.getRole(),
                superAdmin.getUserId()
        ));
    }

    @PostMapping("/stop")
    public ResponseEntity<?> stop(@org.springframework.security.core.annotation.AuthenticationPrincipal CustomUserPrincipal current) {
        if (!current.isImpersonating()) {
            return ResponseEntity.badRequest().body("Not currently impersonating");
        }

        User superAdmin = userRepository.findById(current.getOriginalUserId())
                .orElseThrow(() -> new RuntimeException("Original admin not found"));

        String slug = superAdmin.getTenant() != null ? superAdmin.getTenant().getSlug() : null;
        var tenantId = superAdmin.getTenant() != null ? superAdmin.getTenant().getId() : null;

        CustomUserPrincipal original = new CustomUserPrincipal(
                superAdmin.getId(),
                superAdmin.getEmail(),
                null,
                superAdmin.getRole().name(),
                tenantId,
                slug,
                false,
                null,
                null
        );

        String token = jwtUtils.generateToken(original);

        return ResponseEntity.ok(new AuthResponse(
                token,
                superAdmin.getEmail(),
                superAdmin.getRole().name(),
                superAdmin.getId(),
                tenantId,
                slug,
                false,
                null,
                null
        ));
    }
}