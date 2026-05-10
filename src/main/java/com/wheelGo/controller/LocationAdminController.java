package com.wheelGo.controller;

import com.wheelGo.model.locations.LocationRequest;
import com.wheelGo.model.locations.LocationResponse;
import com.wheelGo.model.locations.LocationUpdateRequest;
import com.wheelGo.service.LocationAdminService;
import com.wheelGo.service.LocationCrudAdminService;
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
@RequestMapping("/api/v1/admin/locations")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class LocationAdminController {

    private final LocationAdminService locationAdminService;
    private final LocationCrudAdminService locationCrudAdminService;

    @GetMapping
    public ResponseEntity<List<LocationResponse>> getAll() {
        return ResponseEntity.ok(locationAdminService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LocationResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(locationCrudAdminService.getById(id));
    }

    @PostMapping
    public ResponseEntity<LocationResponse> create(@RequestBody @Valid LocationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(locationCrudAdminService.create(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<LocationResponse> update(@PathVariable UUID id,
                                                   @RequestBody @Valid LocationUpdateRequest request) {
        return ResponseEntity.ok(locationCrudAdminService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        locationCrudAdminService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
