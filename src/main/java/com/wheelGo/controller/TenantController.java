package com.wheelGo.controller;

import com.wheelGo.model.tenant.TenantRequest;
import com.wheelGo.model.tenant.Tenant;
import com.wheelGo.model.tenant.TenantResponse;
import com.wheelGo.model.tenant.TenantUpdateRequest;
import com.wheelGo.service.TenantService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/super-admin/tenants")
@CrossOrigin(origins = "http://localhost:5173")
@AllArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @PostMapping
    public ResponseEntity<TenantResponse> create(@RequestBody @Valid TenantRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(tenantService.createTenant(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TenantResponse> update(@PathVariable UUID id,
                                                 @RequestBody TenantUpdateRequest request) {
        return ResponseEntity.ok(tenantService.updateTenant(id, request));
    }

    @GetMapping
    public ResponseEntity<List<Tenant>> getAll() {
        return ResponseEntity.ok(tenantService.getAll());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        tenantService.deleteTenant(id);
        return ResponseEntity.noContent().build();
    }
}