package com.wheelGo.controller;

import com.wheelGo.model.tenant.CreateTenantRequest;
import com.wheelGo.model.tenant.Tenant;
import com.wheelGo.model.tenant.TenantResponse;
import com.wheelGo.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping
    public ResponseEntity<TenantResponse> create(
            @RequestBody @Valid CreateTenantRequest request) {
        Tenant tenant = tenantService.createTenant(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(TenantResponse.from(tenant));
    }

    @GetMapping
    public ResponseEntity<List<TenantResponse>> getAll() {
        List<TenantResponse> list = tenantService.getAll()
                .stream()
                .map(TenantResponse::from)
                .toList();
        return ResponseEntity.ok(list);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        tenantService.deleteTenant(id);
        return ResponseEntity.noContent().build();
    }
}