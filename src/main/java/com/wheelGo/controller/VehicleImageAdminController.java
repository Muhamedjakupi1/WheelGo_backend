package com.wheelGo.controller;

import com.wheelGo.model.vehicle_images.VehicleImageResponse;
import com.wheelGo.service.VehicleImageAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
    public ResponseEntity<List<VehicleImageResponse>> getAll(
            @RequestParam(required = false) UUID vehicleId,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ResponseEntity.ok(vehicleImageAdminService.getAll(vehicleId, keyword));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleImageResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(vehicleImageAdminService.getById(id));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VehicleImageResponse> upload(@RequestParam UUID vehicleId,
                                                       @RequestParam MultipartFile file,
                                                       @RequestParam(defaultValue = "false") boolean isPrimary) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vehicleImageAdminService.createFromUpload(vehicleId, file, isPrimary));
    }

    @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VehicleImageResponse> update(@PathVariable UUID id,
                                                       @RequestParam(value = "file", required = false) MultipartFile file,
                                                       @RequestParam(value = "isPrimary", required = false) Boolean isPrimary) {
        return ResponseEntity.ok(vehicleImageAdminService.update(id, file, isPrimary));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        vehicleImageAdminService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
