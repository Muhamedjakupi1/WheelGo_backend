package com.wheelGo.controller;

import com.wheelGo.model.addon.AddonRequest;
import com.wheelGo.model.addon.AddonResponse;
import com.wheelGo.service.AddonAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/addons")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AddonAdminController {
    private final AddonAdminService addonAdminService;

    @GetMapping
    public ResponseEntity<List<AddonResponse>> getAll() {
        return ResponseEntity.ok(addonAdminService.getAll());
    }

    @PostMapping("/ensure-defaults")
    public ResponseEntity<List<AddonResponse>> ensureDefaults() {
        return ResponseEntity.ok(addonAdminService.ensureInventoryAddons());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AddonResponse> update(@PathVariable UUID id, @RequestBody AddonRequest request) {
        return ResponseEntity.ok(addonAdminService.update(id, request));
    }
}
