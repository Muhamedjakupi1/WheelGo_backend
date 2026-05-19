package com.wheelGo.service;

import com.wheelGo.model.bookings.Booking;
import com.wheelGo.model.enums.BookingStatus;
import com.wheelGo.model.enums.MaintenanceType;
import com.wheelGo.model.enums.VehicleStatus;
import com.wheelGo.model.maintenance_records.MaintenanceRecord;
import com.wheelGo.model.maintenance_records.MaintenanceRecordRequest;
import com.wheelGo.model.maintenance_records.MaintenanceRecordResponse;
import com.wheelGo.model.maintenance_records.MaintenanceRecordsUpdateRequest;
import com.wheelGo.model.vehicles.Vehicle;
import com.wheelGo.repository.BookingRepository;
import com.wheelGo.repository.MaintenanceRecordRepository;
import com.wheelGo.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MaintenanceAdminService {
    private static final EnumSet<BookingStatus> BLOCKING_BOOKING_STATUSES =
            EnumSet.of(BookingStatus.CONFIRMED, BookingStatus.ACTIVE);

    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final VehicleRepository vehicleRepository;
    private final BookingRepository bookingRepository;
    private final CacheInvalidationService cacheInvalidationService;

    @Transactional(readOnly = true)
    public List<MaintenanceRecordResponse> getAll() {
        return maintenanceRecordRepository.findAllByOrderByPerformedAtDescCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MaintenanceRecordResponse getById(UUID id) {
        return toResponse(findRecord(id));
    }

    @Transactional(readOnly = true)
    public List<MaintenanceType> getTypes() {
        return List.of(MaintenanceType.values());
    }

    @Transactional
    public MaintenanceRecordResponse create(MaintenanceRecordRequest request) {
        Vehicle vehicle = findVehicle(request.getVehicleId());

        MaintenanceRecord record = new MaintenanceRecord();
        record.setVehicle(vehicle);
        apply(record, request.getType(), request.getDescription(), request.getCost(), request.getPerformedAt(), request.getNextDueAt(), request.getPerformedBy());

        MaintenanceRecord saved = maintenanceRecordRepository.save(record);
        syncVehicleStatusAfterMaintenanceChange(vehicle.getId());
        cacheInvalidationService.evictVehicle(vehicle.getId());
        return toResponse(saved);
    }

    @Transactional
    public MaintenanceRecordResponse update(UUID id, MaintenanceRecordsUpdateRequest request) {
        MaintenanceRecord record = findRecord(id);
        UUID originalVehicleId = record.getVehicle().getId();
        Vehicle vehicle = findVehicle(request.getVehicleId());

        record.setVehicle(vehicle);
        apply(record, request.getType(), request.getDescription(), request.getCost(), request.getPerformedAt(), request.getNextDueAt(), request.getPerformedBy());

        MaintenanceRecord saved = maintenanceRecordRepository.save(record);
        syncVehicleStatusAfterMaintenanceChange(originalVehicleId);
        syncVehicleStatusAfterMaintenanceChange(saved.getVehicle().getId());
        cacheInvalidationService.evictVehicle(originalVehicleId);
        cacheInvalidationService.evictVehicle(saved.getVehicle().getId());
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        MaintenanceRecord record = findRecord(id);
        UUID vehicleId = record.getVehicle().getId();
        maintenanceRecordRepository.delete(record);
        maintenanceRecordRepository.flush();
        syncVehicleStatusAfterMaintenanceChange(vehicleId);
        cacheInvalidationService.evictVehicle(vehicleId);
    }

    private MaintenanceRecord findRecord(UUID id) {
        return maintenanceRecordRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Maintenance record not found"));
    }

    private Vehicle findVehicle(UUID id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found"));
    }

    private void apply(MaintenanceRecord record,
                       MaintenanceType type,
                       String description,
                       java.math.BigDecimal cost,
                       LocalDateTime performedAt,
                       LocalDateTime nextDueAt,
                       String performedBy) {
        if (type == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type of service is required");
        }
        if (performedAt == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Performed date is required");
        }
        if (cost != null && cost.signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cost cannot be negative");
        }
        if (nextDueAt != null && nextDueAt.isBefore(performedAt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Next due date cannot be before performed date");
        }

        record.setType(type);
        record.setDescription(trimToNull(description));
        record.setCost(cost);
        record.setPerformedAt(performedAt);
        record.setNextDueAt(nextDueAt);
        record.setPerformedBy(trimToNull(performedBy));
    }

    private MaintenanceRecordResponse toResponse(MaintenanceRecord record) {
        MaintenanceRecordResponse response = new MaintenanceRecordResponse();
        Vehicle vehicle = record.getVehicle();
        response.setId(record.getId());
        response.setVehicleId(vehicle != null ? vehicle.getId() : null);
        response.setVehicleName(vehicle != null ? vehicle.getMake() + " " + vehicle.getModel() : null);
        response.setPlateNumber(vehicle != null ? vehicle.getPlateNumber() : null);
        response.setType(record.getType());
        response.setDescription(record.getDescription());
        response.setCost(record.getCost());
        response.setPerformedAt(record.getPerformedAt());
        response.setNextDueAt(record.getNextDueAt());
        response.setPerformedBy(record.getPerformedBy());
        response.setCreatedAt(record.getCreatedAt());
        return response;
    }

    private void forceVehicleIntoMaintenance(Vehicle vehicle) {
        if (vehicle.getStatus() != VehicleStatus.MAINTENANCE) {
            vehicle.setStatus(VehicleStatus.MAINTENANCE);
            vehicle.setUpdatedAt(LocalDateTime.now());
            vehicleRepository.save(vehicle);
        }
    }

    private void syncVehicleStatusAfterMaintenanceChange(UUID vehicleId) {
        if (vehicleId == null) {
            return;
        }

        Vehicle vehicle = vehicleRepository.findById(vehicleId).orElse(null);
        if (vehicle == null) {
            return;
        }

        MaintenanceAvailability maintenanceAvailability = resolveMaintenanceAvailability(vehicleId);
        if (maintenanceAvailability.active()) {
            forceVehicleIntoMaintenance(vehicle);
            return;
        }

        if (vehicle.getStatus() == VehicleStatus.INACTIVE) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        Booking activeBooking = bookingRepository.findAllByVehicleIdAndStatusInOrderByEndDateAsc(vehicleId, BLOCKING_BOOKING_STATUSES)
                .stream()
                .filter(booking -> !booking.getStartDate().isAfter(now) && !booking.getEndDate().isBefore(now))
                .findFirst()
                .orElse(null);

        VehicleStatus nextStatus = activeBooking != null ? VehicleStatus.RENTED : VehicleStatus.AVAILABLE;
        if (vehicle.getStatus() != nextStatus) {
            vehicle.setStatus(nextStatus);
            vehicle.setUpdatedAt(LocalDateTime.now());
            vehicleRepository.save(vehicle);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private MaintenanceAvailability resolveMaintenanceAvailability(UUID vehicleId) {
        List<MaintenanceRecord> records = maintenanceRecordRepository.findAllByVehicle_IdOrderByPerformedAtDescCreatedAtDesc(vehicleId);
        if (records.isEmpty()) {
            return new MaintenanceAvailability(false, null);
        }

        if (records.stream().anyMatch(record -> record.getNextDueAt() == null)) {
            return new MaintenanceAvailability(true, null);
        }

        LocalDateTime availableFrom = records.stream()
                .map(MaintenanceRecord::getNextDueAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        boolean active = availableFrom != null && LocalDateTime.now().toLocalDate().isBefore(availableFrom.toLocalDate());
        return new MaintenanceAvailability(active, availableFrom);
    }

    private record MaintenanceAvailability(boolean active, LocalDateTime availableFrom) {
    }
}
