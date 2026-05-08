package com.wheelGo.controller;

import com.wheelGo.model.vehicle_images.VehicleImageRequest;
import com.wheelGo.model.vehicle_images.VehicleImageResponse;
import com.wheelGo.model.vehicle_images.VehicleImagesUpdateRequest;
import com.wheelGo.service.VehicleImageAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/vehicle-images")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class VehicleImageAdminController {

    private final VehicleImageAdminService vehicleImageAdminService;

    @GetMapping
    public ResponseEntity<List<VehicleImageResponse>> getAll(@RequestParam(required = false) UUID vehicleId) {
        return ResponseEntity.ok(vehicleImageAdminService.getAll(vehicleId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleImageResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(vehicleImageAdminService.getById(id));
    }

    @PostMapping
    public ResponseEntity<VehicleImageResponse> create(@RequestBody @Valid VehicleImageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleImageAdminService.create(request));
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<VehicleImageResponse> upload(@RequestParam UUID vehicleId,
                                                       @RequestParam MultipartFile file,
                                                       @RequestParam(defaultValue = "false") boolean isPrimary) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vehicleImageAdminService.createFromUpload(vehicleId, file, isPrimary));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<VehicleImageResponse> update(@PathVariable UUID id,
                                                       @RequestBody VehicleImagesUpdateRequest request) {
        return ResponseEntity.ok(vehicleImageAdminService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        vehicleImageAdminService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
