package com.wheelGo.controller;

import com.wheelGo.auth.AuthResponse;
import com.wheelGo.security.CustomUserPrincipal;
import com.wheelGo.service.ImpersonationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/super-admin/impersonation")
@CrossOrigin(origins = "http://localhost:5173")
public class ImpersonationController {

    private final ImpersonationService impersonationService;

    public ImpersonationController(ImpersonationService impersonationService) {
        this.impersonationService = impersonationService;
    }

    @PostMapping("/start/{tenantSlug}")
    @PreAuthorize("hasRole('SUPER_ADMIN') and !principal.impersonating")
    public ResponseEntity<?> start(@PathVariable String tenantSlug,
                                   @org.springframework.security.core.annotation.AuthenticationPrincipal CustomUserPrincipal superAdmin) {
        return ResponseEntity.ok(impersonationService.startForTenant(tenantSlug, superAdmin));
    }

    @PostMapping("/start/{tenantSlug}/{targetUserId}")
    @PreAuthorize("hasRole('SUPER_ADMIN') and !principal.impersonating")
    public ResponseEntity<?> start(@PathVariable String tenantSlug,
                                   @PathVariable UUID targetUserId,
                                   @org.springframework.security.core.annotation.AuthenticationPrincipal CustomUserPrincipal superAdmin) {
        return ResponseEntity.ok(impersonationService.startForTenant(tenantSlug, superAdmin));
    }

    @PostMapping("/stop")
    @PreAuthorize("isAuthenticated() and principal.impersonating")
    public ResponseEntity<?> stop(@org.springframework.security.core.annotation.AuthenticationPrincipal CustomUserPrincipal current) {
        return ResponseEntity.ok(impersonationService.stop(current));
    }
}
