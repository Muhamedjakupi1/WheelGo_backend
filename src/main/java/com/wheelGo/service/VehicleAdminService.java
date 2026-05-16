package com.wheelGo.service;

import com.wheelGo.model.locations.Location;
import com.wheelGo.model.enums.BookingStatus;
import com.wheelGo.model.vehicle_categories.VehicleCategory;
import com.wheelGo.model.vehicle_images.VehicleImage;
import com.wheelGo.model.vehicles.Vehicle;
import com.wheelGo.model.vehicles.VehicleRequest;
import com.wheelGo.model.vehicles.VehicleResponse;
import com.wheelGo.model.vehicles.VehicleUpdateRequest;
import com.wheelGo.repository.BookingRepository;
import com.wheelGo.repository.LocationRepository;
import com.wheelGo.repository.VehicleCategoryRepository;
import com.wheelGo.repository.VehicleImageRepository;
import com.wheelGo.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VehicleAdminService {
    private static final String VEHICLE_IN_USE_MESSAGE =
            "This vehicle cannot be deleted because it is already used in one or more bookings.";
    private static final EnumSet<BookingStatus> BLOCKING_BOOKING_STATUSES =
            EnumSet.of(BookingStatus.CONFIRMED, BookingStatus.ACTIVE);
    private static final DateTimeFormatter RENTED_UNTIL_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final VehicleRepository vehicleRepository;
    private final VehicleCategoryRepository vehicleCategoryRepository;
    private final LocationRepository locationRepository;
    private final VehicleImageRepository vehicleImageRepository;
    private final BookingRepository bookingRepository;

    @Transactional(readOnly = true)
    public List<VehicleResponse> getAll() {
        List<Vehicle> vehicles = vehicleRepository.findAllByOrderByCreatedAtDesc();
        return toResponses(vehicles);
    }

    @Transactional(readOnly = true)
    public VehicleResponse getById(UUID id) {
        return toResponses(List.of(findVehicle(id))).getFirst();
    }

    @Transactional
    public VehicleResponse create(VehicleRequest request) {
        validateUniqueFields(request.getPlateNumber(), request.getVin(), null);

        Vehicle vehicle = new Vehicle();
        applyCreateOrUpdate(vehicle, request.getCategoryId(), request.getLocationId(), request.getPlateNumber(),
                request.getMake(), request.getModel(), request.getYear(), request.getColor(), request.getVin(),
                request.getFuelType(), request.getTransmission(), request.getSeats(), request.getDailyRate(),
                request.getMileage(), null);
        return toResponse(vehicleRepository.save(vehicle));
    }

    @Transactional
    public VehicleResponse update(UUID id, VehicleUpdateRequest request) {
        Vehicle vehicle = findVehicle(id);

        String plateNumber = request.getPlateNumber() != null ? request.getPlateNumber() : vehicle.getPlateNumber();
        String vin = request.getVin() != null ? request.getVin() : vehicle.getVin();
        validateUniqueFields(plateNumber, vin, id);

        applyCreateOrUpdate(
                vehicle,
                request.getCategoryId() != null ? request.getCategoryId() : vehicle.getCategory().getId(),
                Boolean.TRUE.equals(request.getClearLocation())
                        ? null
                        : request.getLocationId() != null
                        ? request.getLocationId()
                        : vehicle.getLocation() != null ? vehicle.getLocation().getId() : null,
                plateNumber,
                request.getMake() != null ? request.getMake() : vehicle.getMake(),
                request.getModel() != null ? request.getModel() : vehicle.getModel(),
                request.getYear() != null ? request.getYear() : vehicle.getYear(),
                request.getColor() != null ? request.getColor() : vehicle.getColor(),
                vin,
                request.getFuelType() != null ? request.getFuelType() : vehicle.getFuelType(),
                request.getTransmission() != null ? request.getTransmission() : vehicle.getTransmission(),
                request.getSeats() != null ? request.getSeats() : vehicle.getSeats(),
                request.getDailyRate() != null ? request.getDailyRate() : vehicle.getDailyRate(),
                request.getMileage() != null ? request.getMileage() : vehicle.getMileage(),
                request.getStatus() != null ? request.getStatus() : vehicle.getStatus()
        );

        return toResponse(vehicleRepository.save(vehicle));
    }

    @Transactional
    public void delete(UUID id) {
        Vehicle vehicle = findVehicle(id);
        try {
            vehicleRepository.delete(vehicle);
            vehicleRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, VEHICLE_IN_USE_MESSAGE, ex);
        }
    }

    private void applyCreateOrUpdate(Vehicle vehicle,
                                     UUID categoryId,
                                     UUID locationId,
                                     String plateNumber,
                                     String make,
                                     String model,
                                     Short year,
                                     String color,
                                     String vin,
                                     com.wheelGo.model.enums.FuelType fuelType,
                                     com.wheelGo.model.enums.Transmission transmission,
                                     Short seats,
                                     java.math.BigDecimal dailyRate,
                                     Integer mileage,
                                     com.wheelGo.model.enums.VehicleStatus status) {
        vehicle.setCategory(findCategory(categoryId));
        vehicle.setLocation(findLocation(locationId));
        vehicle.setPlateNumber(requiredTrimmed(plateNumber, "Plate number is required"));
        vehicle.setMake(requiredTrimmed(make, "Vehicle make is required"));
        vehicle.setModel(requiredTrimmed(model, "Vehicle model is required"));
        vehicle.setYear(year);
        vehicle.setColor(trimToNull(color));
        vehicle.setVin(trimToNull(vin));
        vehicle.setFuelType(fuelType != null ? fuelType : com.wheelGo.model.enums.FuelType.PETROL);
        vehicle.setTransmission(transmission != null ? transmission : com.wheelGo.model.enums.Transmission.MANUAL);
        vehicle.setSeats(seats != null ? seats : 5);
        vehicle.setDailyRate(dailyRate);
        vehicle.setMileage(mileage != null ? mileage : 0);
        if (status != null) {
            vehicle.setStatus(status);
        }
    }

    private void validateUniqueFields(String plateNumber, String vin, UUID currentId) {
        String normalizedPlate = requiredTrimmed(plateNumber, "Plate number is required");
        if (currentId == null
                ? vehicleRepository.existsByPlateNumberIgnoreCase(normalizedPlate)
                : vehicleRepository.existsByPlateNumberIgnoreCaseAndIdNot(normalizedPlate, currentId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vehicle plate number already exists");
        }

        String normalizedVin = trimToNull(vin);
        if (normalizedVin != null) {
            boolean vinExists = currentId == null
                    ? vehicleRepository.existsByVinIgnoreCase(normalizedVin)
                    : vehicleRepository.existsByVinIgnoreCaseAndIdNot(normalizedVin, currentId);
            if (vinExists) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vehicle VIN already exists");
            }
        }
    }

    private Vehicle findVehicle(UUID id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found"));
    }

    private VehicleCategory findCategory(UUID id) {
        return vehicleCategoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vehicle category not found"));
    }

    private Location findLocation(UUID id) {
        if (id == null) {
            return null;
        }
        return locationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Location not found"));
    }

    private VehicleResponse toResponse(Vehicle vehicle) {
        return toResponses(List.of(vehicle)).getFirst();
    }

    private List<VehicleResponse> toResponses(List<Vehicle> vehicles) {
        List<UUID> vehicleIds = vehicles.stream().map(Vehicle::getId).toList();
        Map<UUID, List<VehicleImage>> imagesByVehicleId = vehicleIds.isEmpty()
                ? Map.of()
                : vehicleImageRepository.findByVehicleIdInOrderByUploadedAtDesc(vehicleIds)
                .stream()
                .collect(Collectors.groupingBy(image -> image.getVehicle().getId()));

        return vehicles.stream()
                .map(vehicle -> toResponse(vehicle, imagesByVehicleId.getOrDefault(vehicle.getId(), List.of())))
                .toList();
    }

    private VehicleResponse toResponse(Vehicle vehicle, List<VehicleImage> images) {
        VehicleResponse response = new VehicleResponse();
        response.setId(vehicle.getId());
        response.setCategoryId(vehicle.getCategory() != null ? vehicle.getCategory().getId() : null);
        response.setCategoryName(vehicle.getCategory() != null ? vehicle.getCategory().getName() : null);
        response.setLocationId(vehicle.getLocation() != null ? vehicle.getLocation().getId() : null);
        response.setLocationName(vehicle.getLocation() != null ? vehicle.getLocation().getName() : null);
        response.setPlateNumber(vehicle.getPlateNumber());
        response.setMake(vehicle.getMake());
        response.setModel(vehicle.getModel());
        response.setYear(vehicle.getYear());
        response.setColor(vehicle.getColor());
        response.setVin(vehicle.getVin());
        response.setFuelType(vehicle.getFuelType());
        response.setTransmission(vehicle.getTransmission());
        response.setSeats(vehicle.getSeats());
        response.setDailyRate(vehicle.getDailyRate());
        response.setStatus(vehicle.getStatus());
        bookingRepository.findAllByVehicleIdAndStatusInOrderByEndDateAsc(vehicle.getId(), BLOCKING_BOOKING_STATUSES)
                .stream()
                .map(booking -> booking.getEndDate().toLocalDate())
                .max(java.time.LocalDate::compareTo)
                .ifPresent(rentedUntil -> {
                    response.setRentedUntil(rentedUntil);
                    response.setStatusMessage("Rented until " + rentedUntil.format(RENTED_UNTIL_FORMATTER));
                });
        response.setMileage(vehicle.getMileage());
        response.setImageUrls(images.stream().map(VehicleImage::getUrl).toList());
        response.setPrimaryImageUrl(images.stream()
                .filter(VehicleImage::isPrimary)
                .findFirst()
                .or(() -> images.stream().findFirst())
                .map(VehicleImage::getUrl)
                .orElse(null));
        return response;
    }

    private String requiredTrimmed(String value, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
