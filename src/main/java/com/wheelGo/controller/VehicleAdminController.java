package com.wheelGo.controller;

import com.wheelGo.model.bookings.BookingResponse;
import com.wheelGo.model.vehicles.VehicleRequest;
import com.wheelGo.model.vehicles.VehicleResponse;
import com.wheelGo.model.vehicles.VehicleUpdateRequest;
import com.wheelGo.service.VehicleAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/vehicles")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class VehicleAdminController {

    private final VehicleAdminService vehicleAdminService;

    @GetMapping
    public ResponseEntity<List<VehicleResponse>> getAll(
            @RequestParam(value = "keyword", required = false) String keyword) {

        if (keyword != null && !keyword.trim().isEmpty()) {
            return ResponseEntity.ok(vehicleAdminService.searchVehicle(keyword));
        }
        return ResponseEntity.ok(vehicleAdminService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(vehicleAdminService.getById(id));
    }

    @PostMapping
    public ResponseEntity<VehicleResponse> create(@RequestBody @Valid VehicleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleAdminService.create(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<VehicleResponse> update(@PathVariable UUID id,
                                                  @RequestBody @Valid VehicleUpdateRequest request) {
        return ResponseEntity.ok(vehicleAdminService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        vehicleAdminService.delete(id);
        return ResponseEntity.noContent().build();
    }

        @GetMapping("/")
    public ResponseEntity<List<VehicleResponse>> searchBooking(String keyword){
        List<VehicleResponse> vehicles = vehicleAdminService.searchVehicle(keyword);
        return new ResponseEntity<>(vehicles, HttpStatus.OK);
    }
}
