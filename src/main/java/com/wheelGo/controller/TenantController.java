package com.wheelGo.controller;

import com.wheelGo.model.tenant.TenantRequest;
import com.wheelGo.model.tenant.Tenant;
import com.wheelGo.model.tenant.TenantResponse;
import com.wheelGo.model.tenant.TenantUpdateRequest;
import com.wheelGo.model.tenant_settings.SupportedCurrencyResponse;
import com.wheelGo.security.ApiErrorResponse;
import com.wheelGo.service.SupportedCurrencyService;
import com.wheelGo.service.TenantService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/super-admin/tenants")
@CrossOrigin(origins = "http://localhost:5173")
@AllArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN')")

public class TenantController {

    private final TenantService tenantService;
    private final SupportedCurrencyService supportedCurrencyService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody @Valid TenantRequest request) {
        try {
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(tenantService.createTenant(request));
        } catch (ResponseStatusException e) {
            return buildErrorResponse(HttpStatus.valueOf(e.getStatusCode().value()), e.getReason(), "/api/super-admin/tenants");
        } catch (RuntimeException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage(), "/api/super-admin/tenants");
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TenantResponse> update(@PathVariable UUID id,
                                                 @RequestBody TenantUpdateRequest request) {
        return ResponseEntity.ok(tenantService.updateTenant(id, request));
    }

    @GetMapping
    public ResponseEntity<List<TenantResponse>> getAll() {
        return ResponseEntity.ok(tenantService.getAll());
    }

    @GetMapping("/currencies")
    public ResponseEntity<List<SupportedCurrencyResponse>> getSupportedCurrencies() {
        return ResponseEntity.ok(supportedCurrencyService.getSupportedCurrencies());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        tenantService.deleteTenant(id);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<ApiErrorResponse> buildErrorResponse(HttpStatus status, String message, String path) {
        return ResponseEntity.status(status)
                .body(ApiErrorResponse.of(
                        status.value(),
                        status.getReasonPhrase(),
                        message,
                        path
                ));
    }
}
