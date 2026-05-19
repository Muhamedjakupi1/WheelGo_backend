package com.wheelGo.controller;

import com.wheelGo.model.vehicle_categories.VehicleCategoryRequest;
import com.wheelGo.model.vehicle_categories.VehicleCategoryResponse;
import com.wheelGo.model.vehicle_categories.VehicleCategoryUpdateRequest;
import com.wheelGo.service.VehicleCategoryAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/vehicle-categories")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class VehicleCategoryAdminController {

    private final VehicleCategoryAdminService vehicleCategoryAdminService;

    @GetMapping
    public ResponseEntity<List<VehicleCategoryResponse>> getAll(
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ResponseEntity.ok(vehicleCategoryAdminService.getAll(keyword));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleCategoryResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(vehicleCategoryAdminService.getById(id));
    }

    @PostMapping
    public ResponseEntity<VehicleCategoryResponse> create(@RequestBody @Valid VehicleCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleCategoryAdminService.create(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<VehicleCategoryResponse> update(@PathVariable UUID id,
                                                          @RequestBody @Valid VehicleCategoryUpdateRequest request) {
        return ResponseEntity.ok(vehicleCategoryAdminService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        vehicleCategoryAdminService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
