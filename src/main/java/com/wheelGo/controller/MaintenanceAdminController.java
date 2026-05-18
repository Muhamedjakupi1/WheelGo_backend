package com.wheelGo.controller;

import com.wheelGo.model.enums.MaintenanceType;
import com.wheelGo.model.maintenance_records.MaintenanceRecordRequest;
import com.wheelGo.model.maintenance_records.MaintenanceRecordResponse;
import com.wheelGo.model.maintenance_records.MaintenanceRecordsUpdateRequest;
import com.wheelGo.service.MaintenanceAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/admin/maintenances")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class MaintenanceAdminController {

    private final MaintenanceAdminService maintenanceAdminService;

    @GetMapping
    public ResponseEntity<List<MaintenanceRecordResponse>> getAll() {
        return ResponseEntity.ok(maintenanceAdminService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaintenanceRecordResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(maintenanceAdminService.getById(id));
    }

    @GetMapping("/types")
    public ResponseEntity<List<MaintenanceType>> getTypes() {
        return ResponseEntity.ok(maintenanceAdminService.getTypes());
    }

    @PostMapping
    public ResponseEntity<MaintenanceRecordResponse> create(@RequestBody @Valid MaintenanceRecordRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(maintenanceAdminService.create(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<MaintenanceRecordResponse> update(@PathVariable UUID id,
                                                            @RequestBody @Valid MaintenanceRecordsUpdateRequest request) {
        return ResponseEntity.ok(maintenanceAdminService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        maintenanceAdminService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
