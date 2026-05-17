package com.wheelGo.service;

import com.wheelGo.model.bookings.Booking;
import com.wheelGo.model.enums.BookingStatus;
import com.wheelGo.model.locations.Location;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleAdminServiceTest {

    @Mock private VehicleRepository vehicleRepository;
    @Mock private VehicleCategoryRepository vehicleCategoryRepository;
    @Mock private LocationRepository locationRepository;
    @Mock private VehicleImageRepository vehicleImageRepository;
    @Mock private BookingRepository bookingRepository;
    @InjectMocks private VehicleAdminService vehicleAdminService;

    private UUID vehicleId;
    private Vehicle vehicle;
    private VehicleCategory category;
    private Location location;

    @BeforeEach
    void setUp() {
        vehicleId = UUID.randomUUID();
        category = new VehicleCategory();
        category.setId(UUID.randomUUID());
        category.setName("SUV");
        location = new Location();
        location.setId(UUID.randomUUID());
        location.setName("Pristina");
        vehicle = new Vehicle();
        vehicle.setId(vehicleId);
        vehicle.setCategory(category);
        vehicle.setLocation(location);
        vehicle.setPlateNumber("01-123-AA");
        vehicle.setMake("BMW");
        vehicle.setModel("X5");
        vehicle.setYear((short) 2024);
        vehicle.setDailyRate(new BigDecimal("99.99"));
        vehicle.setStatus(com.wheelGo.model.enums.VehicleStatus.AVAILABLE);
        vehicle.setSeats((short) 5);
        vehicle.setMileage(1000);
    }

    @Test
    void should_return_all_vehicles_when_get_all() {
        VehicleImage image = new VehicleImage();
        image.setVehicle(vehicle);
        image.setUrl("/uploads/vehicle.png");
        image.setPrimary(true);
        Booking booking = new Booking();
        booking.setEndDate(LocalDateTime.of(2026, 6, 28, 23, 59));
        booking.setStatus(BookingStatus.CONFIRMED);

        when(vehicleRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(vehicle));
        when(vehicleImageRepository.findByVehicleIdInOrderByUploadedAtDesc(List.of(vehicleId))).thenReturn(List.of(image));
        when(bookingRepository.findAllByVehicleIdAndStatusInOrderByEndDateAsc(any(), any())).thenReturn(List.of(booking));

        List<VehicleResponse> result = vehicleAdminService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getPrimaryImageUrl()).isEqualTo("/uploads/vehicle.png");
        assertThat(result.getFirst().getStatusMessage()).contains("Rented until 28/06/2026");
    }

    @Test
    void should_return_vehicle_when_get_by_id_found() {
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(vehicleImageRepository.findByVehicleIdInOrderByUploadedAtDesc(List.of(vehicleId))).thenReturn(List.of());
        when(bookingRepository.findAllByVehicleIdAndStatusInOrderByEndDateAsc(any(), any())).thenReturn(List.of());

        VehicleResponse result = vehicleAdminService.getById(vehicleId);

        assertThat(result.getPlateNumber()).isEqualTo("01-123-AA");
    }

    @Test
    void should_throw_not_found_when_get_by_id_missing() {
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehicleAdminService.getById(vehicleId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Vehicle not found");
    }

    @Test
    void should_create_vehicle_when_request_valid() {
        VehicleRequest request = new VehicleRequest();
        request.setCategoryId(category.getId());
        request.setLocationId(location.getId());
        request.setPlateNumber("01-123-AA");
        request.setMake("BMW");
        request.setModel("X5");
        request.setYear((short) 2024);
        request.setDailyRate(new BigDecimal("99.99"));

        when(vehicleRepository.existsByPlateNumberIgnoreCase("01-123-AA")).thenReturn(false);
        when(vehicleCategoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(locationRepository.findById(location.getId())).thenReturn(Optional.of(location));
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(vehicle);
        when(vehicleImageRepository.findByVehicleIdInOrderByUploadedAtDesc(List.of(vehicleId))).thenReturn(List.of());
        when(bookingRepository.findAllByVehicleIdAndStatusInOrderByEndDateAsc(any(), any())).thenReturn(List.of());

        VehicleResponse result = vehicleAdminService.create(request);

        assertThat(result.getMake()).isEqualTo("BMW");
    }

    @Test
    void should_throw_bad_request_when_plate_already_exists_on_create() {
        VehicleRequest request = new VehicleRequest();
        request.setPlateNumber("01-123-AA");

        when(vehicleRepository.existsByPlateNumberIgnoreCase("01-123-AA")).thenReturn(true);

        assertThatThrownBy(() -> vehicleAdminService.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Vehicle plate number already exists");
    }

    @Test
    void should_throw_bad_request_when_category_missing_on_create() {
        VehicleRequest request = new VehicleRequest();
        request.setCategoryId(category.getId());
        request.setPlateNumber("01-123-AA");
        request.setMake("BMW");
        request.setModel("X5");

        when(vehicleRepository.existsByPlateNumberIgnoreCase("01-123-AA")).thenReturn(false);
        when(vehicleCategoryRepository.findById(category.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehicleAdminService.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Vehicle category not found");
    }

    @Test
    void should_update_vehicle_when_request_valid() {
        VehicleUpdateRequest request = new VehicleUpdateRequest();
        request.setMake("Audi");
        request.setPlateNumber("01-123-AA");
        request.setCategoryId(category.getId());

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(vehicleRepository.existsByPlateNumberIgnoreCaseAndIdNot("01-123-AA", vehicleId)).thenReturn(false);
        when(vehicleCategoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(locationRepository.findById(location.getId())).thenReturn(Optional.of(location));
        when(vehicleRepository.save(vehicle)).thenReturn(vehicle);
        when(vehicleImageRepository.findByVehicleIdInOrderByUploadedAtDesc(List.of(vehicleId))).thenReturn(List.of());
        when(bookingRepository.findAllByVehicleIdAndStatusInOrderByEndDateAsc(any(), any())).thenReturn(List.of());

        VehicleResponse result = vehicleAdminService.update(vehicleId, request);

        assertThat(result.getMake()).isEqualTo("Audi");
    }

    @Test
    void should_throw_conflict_when_delete_violates_integrity() {
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        doThrow(new DataIntegrityViolationException("boom")).when(vehicleRepository).flush();

        assertThatThrownBy(() -> vehicleAdminService.delete(vehicleId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already used in one or more bookings");
    }
}
