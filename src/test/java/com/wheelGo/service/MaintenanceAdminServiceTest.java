package com.wheelGo.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaintenanceAdminServiceTest {

    @Mock private MaintenanceRecordRepository maintenanceRecordRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private CacheInvalidationService cacheInvalidationService;
    @InjectMocks private MaintenanceAdminService maintenanceAdminService;

    private UUID vehicleId;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        vehicleId = UUID.randomUUID();
        vehicle = new Vehicle();
        vehicle.setId(vehicleId);
        vehicle.setMake("BMW");
        vehicle.setModel("X5");
        vehicle.setPlateNumber("01-123-AA");
        vehicle.setStatus(VehicleStatus.AVAILABLE);
    }

    @Test
    void should_create_record_and_move_vehicle_to_maintenance_when_request_valid() {
        MaintenanceRecordRequest request = new MaintenanceRecordRequest();
        request.setVehicleId(vehicleId);
        request.setType(MaintenanceType.REPAIR);
        request.setDescription("Brake work");
        request.setCost(new BigDecimal("120.00"));
        request.setPerformedAt(LocalDateTime.of(2026, 5, 17, 10, 0));
        request.setPerformedBy("Garage");

        MaintenanceRecord saved = new MaintenanceRecord();
        saved.setId(UUID.randomUUID());
        saved.setVehicle(vehicle);
        saved.setType(request.getType());
        saved.setDescription(request.getDescription());
        saved.setCost(request.getCost());
        saved.setPerformedAt(request.getPerformedAt());
        saved.setPerformedBy(request.getPerformedBy());

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(maintenanceRecordRepository.save(any(MaintenanceRecord.class))).thenReturn(saved);
        when(maintenanceRecordRepository.findAllByVehicle_IdOrderByPerformedAtDescCreatedAtDesc(vehicleId)).thenReturn(List.of(saved));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MaintenanceRecordResponse response = maintenanceAdminService.create(request);

        assertThat(response.getType()).isEqualTo(MaintenanceType.REPAIR);
        assertThat(response.getVehicleId()).isEqualTo(vehicleId);
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.MAINTENANCE);
    }

    @Test
    void should_throw_not_found_when_create_vehicle_missing() {
        MaintenanceRecordRequest request = new MaintenanceRecordRequest();
        request.setVehicleId(vehicleId);
        request.setType(MaintenanceType.REPAIR);
        request.setPerformedAt(LocalDateTime.now());

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> maintenanceAdminService.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Vehicle not found");
    }

    @Test
    void should_update_record_vehicle_when_request_changes_vehicle() {
        UUID otherVehicleId = UUID.randomUUID();
        Vehicle otherVehicle = new Vehicle();
        otherVehicle.setId(otherVehicleId);
        otherVehicle.setMake("Audi");
        otherVehicle.setModel("A4");
        otherVehicle.setPlateNumber("02-222-BB");
        otherVehicle.setStatus(VehicleStatus.AVAILABLE);

        MaintenanceRecord record = new MaintenanceRecord();
        record.setId(UUID.randomUUID());
        record.setVehicle(vehicle);
        record.setType(MaintenanceType.OIL_CHANGE);
        record.setPerformedAt(LocalDateTime.of(2026, 5, 17, 9, 0));

        MaintenanceRecordsUpdateRequest request = new MaintenanceRecordsUpdateRequest();
        request.setVehicleId(otherVehicleId);
        request.setType(MaintenanceType.INSPECTION);
        request.setCost(new BigDecimal("45.00"));
        request.setPerformedAt(LocalDateTime.of(2026, 5, 18, 11, 0));

        when(maintenanceRecordRepository.findById(record.getId())).thenReturn(Optional.of(record));
        when(vehicleRepository.findById(otherVehicleId)).thenReturn(Optional.of(otherVehicle));
        when(maintenanceRecordRepository.save(record)).thenReturn(record);
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(maintenanceRecordRepository.findAllByVehicle_IdOrderByPerformedAtDescCreatedAtDesc(vehicleId)).thenReturn(List.of());
        when(maintenanceRecordRepository.findAllByVehicle_IdOrderByPerformedAtDescCreatedAtDesc(otherVehicleId)).thenReturn(List.of(record));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingRepository.findAllByVehicleIdAndStatusInOrderByEndDateAsc(vehicleId, java.util.EnumSet.of(com.wheelGo.model.enums.BookingStatus.CONFIRMED, com.wheelGo.model.enums.BookingStatus.ACTIVE)))
                .thenReturn(List.of());

        MaintenanceRecordResponse response = maintenanceAdminService.update(record.getId(), request);

        assertThat(response.getVehicleId()).isEqualTo(otherVehicleId);
        assertThat(record.getVehicle().getId()).isEqualTo(otherVehicleId);
        assertThat(otherVehicle.getStatus()).isEqualTo(VehicleStatus.MAINTENANCE);
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
    }

    @Test
    void should_delete_record_and_restore_vehicle_availability_when_no_other_records_exist() {
        MaintenanceRecord record = new MaintenanceRecord();
        record.setId(UUID.randomUUID());
        record.setVehicle(vehicle);

        when(maintenanceRecordRepository.findById(record.getId())).thenReturn(Optional.of(record));
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(maintenanceRecordRepository.findAllByVehicle_IdOrderByPerformedAtDescCreatedAtDesc(vehicleId)).thenReturn(List.of());
        when(bookingRepository.findAllByVehicleIdAndStatusInOrderByEndDateAsc(any(), any())).thenReturn(List.of());

        maintenanceAdminService.delete(record.getId());

        verify(maintenanceRecordRepository).delete(record);
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
    }

    @Test
    void should_restore_vehicle_availability_when_maintenance_end_date_is_today() {
        LocalDateTime availableAgain = LocalDateTime.now().withHour(8).withMinute(0).withSecond(0).withNano(0);
        MaintenanceRecordRequest request = new MaintenanceRecordRequest();
        request.setVehicleId(vehicleId);
        request.setType(MaintenanceType.REPAIR);
        request.setPerformedAt(availableAgain.minusDays(1));
        request.setNextDueAt(availableAgain);

        MaintenanceRecord saved = new MaintenanceRecord();
        saved.setId(UUID.randomUUID());
        saved.setVehicle(vehicle);
        saved.setType(request.getType());
        saved.setPerformedAt(request.getPerformedAt());
        saved.setNextDueAt(request.getNextDueAt());

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(maintenanceRecordRepository.save(any(MaintenanceRecord.class))).thenReturn(saved);
        when(maintenanceRecordRepository.findAllByVehicle_IdOrderByPerformedAtDescCreatedAtDesc(vehicleId)).thenReturn(List.of(saved));
        when(bookingRepository.findAllByVehicleIdAndStatusInOrderByEndDateAsc(vehicleId, java.util.EnumSet.of(com.wheelGo.model.enums.BookingStatus.CONFIRMED, com.wheelGo.model.enums.BookingStatus.ACTIVE)))
                .thenReturn(List.of());

        maintenanceAdminService.create(request);

        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
    }
}
