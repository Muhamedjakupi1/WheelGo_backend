package com.wheelGo.service;

import com.wheelGo.model.bookings.Booking;
import com.wheelGo.model.bookings.BookingAdminDecisionRequest;
import com.wheelGo.model.bookings.BookingCreateRequest;
import com.wheelGo.model.bookings.BookingResponse;
import com.wheelGo.model.driver_licenses.DriverLicense;
import com.wheelGo.model.enums.BookingStatus;
import com.wheelGo.model.enums.PaymentStatus;
import com.wheelGo.model.enums.VehicleStatus;
import com.wheelGo.model.maintenance_records.MaintenanceRecord;
import com.wheelGo.model.locations.Location;
import com.wheelGo.model.payments.Payment;
import com.wheelGo.model.user.User;
import com.wheelGo.model.vehicles.Vehicle;
import com.wheelGo.repository.AddonRepository;
import com.wheelGo.repository.BookingAddonRepository;
import com.wheelGo.repository.BookingRepository;
import com.wheelGo.repository.DriverLicenseRepository;
import com.wheelGo.repository.InvoiceRepository;
import com.wheelGo.repository.MaintenanceRecordRepository;
import com.wheelGo.repository.PaymentRepository;
import com.wheelGo.repository.TenantRepository;
import com.wheelGo.repository.UserRepository;
import com.wheelGo.repository.VehicleImageRepository;
import com.wheelGo.repository.VehicleRepository;
import com.wheelGo.schema.TenantSchemaExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private BookingAddonRepository bookingAddonRepository;
    @Mock private AddonRepository addonRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private TenantSchemaExecutor tenantSchemaExecutor;
    @Mock private UserRepository userRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private VehicleImageRepository vehicleImageRepository;
    @Mock private MaintenanceRecordRepository maintenanceRecordRepository;
    @Mock private CacheInvalidationService cacheInvalidationService;
    @Mock private PaymentRepository paymentRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private DriverLicenseRepository driverLicenseRepository;
    @InjectMocks private BookingService bookingService;

    private UUID userId;
    private UUID vehicleId;
    private User user;
    private Vehicle vehicle;
    private DriverLicense verifiedLicense;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        vehicleId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        verifiedLicense = new DriverLicense();
        verifiedLicense.setUser(user);
        verifiedLicense.setVerified(true);
        verifiedLicense.setExpiryDate(LocalDate.now().plusYears(1));
        Location location = new Location();
        location.setId(UUID.randomUUID());
        location.setName("Pristina");
        vehicle = new Vehicle();
        vehicle.setId(vehicleId);
        vehicle.setLocation(location);
        vehicle.setMake("BMW");
        vehicle.setModel("X5");
        vehicle.setDailyRate(new BigDecimal("100.00"));
        vehicle.setStatus(VehicleStatus.AVAILABLE);
    }

    @Test
    void should_create_booking_when_request_valid() {
        BookingCreateRequest request = new BookingCreateRequest();
        request.setVehicleId(vehicleId);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(3));

        Booking saved = new Booking();
        saved.setId(UUID.randomUUID());
        saved.setUserId(userId);
        saved.setVehicleId(vehicleId);
        saved.setPickupLocationId(vehicle.getLocation().getId());
        saved.setDropoffLocationId(vehicle.getLocation().getId());
        saved.setStartDate(request.getStartDate().atStartOfDay());
        saved.setEndDate(request.getEndDate().atTime(java.time.LocalTime.MAX));
        saved.setTotalDays(3);
        saved.setBasePrice(new BigDecimal("300.00"));
        saved.setDiscountAmount(BigDecimal.ZERO);
        saved.setAddonPrice(BigDecimal.ZERO);
        saved.setTotalPrice(new BigDecimal("300.00"));
        saved.setStatus(BookingStatus.PENDING);

        when(bookingRepository.findAllByStatusInAndEndDateBefore(any(), any())).thenReturn(List.of());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(driverLicenseRepository.findByUser_Id(userId)).thenReturn(Optional.of(verifiedLicense));
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(maintenanceRecordRepository.findAllByVehicle_IdOrderByPerformedAtDescCreatedAtDesc(vehicleId)).thenReturn(List.of());
        when(bookingRepository.findAllByVehicleIdAndStatusInAndStartDateLessThanAndEndDateGreaterThanOrderByEndDateAsc(any(), any(), any(), any())).thenReturn(List.of());
        when(bookingRepository.save(any(Booking.class))).thenReturn(saved);
        when(vehicleImageRepository.findByVehicleIdOrderByUploadedAtDesc(vehicleId)).thenReturn(List.of());

        var result = bookingService.createBooking(userId, request);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(result.getVehicleName()).isEqualTo("BMW X5");
    }

    @Test
    void should_throw_bad_request_when_vehicle_has_no_location() {
        vehicle.setLocation(null);
        BookingCreateRequest request = new BookingCreateRequest();
        request.setVehicleId(vehicleId);

        when(bookingRepository.findAllByStatusInAndEndDateBefore(any(), any())).thenReturn(List.of());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(driverLicenseRepository.findByUser_Id(userId)).thenReturn(Optional.of(verifiedLicense));
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> bookingService.createBooking(userId, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Vehicle location is not configured");
    }

    @Test
    void should_throw_bad_request_when_vehicle_inactive_for_booking() {
        vehicle.setStatus(VehicleStatus.INACTIVE);
        BookingCreateRequest request = new BookingCreateRequest();
        request.setVehicleId(vehicleId);

        when(bookingRepository.findAllByStatusInAndEndDateBefore(any(), any())).thenReturn(List.of());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(driverLicenseRepository.findByUser_Id(userId)).thenReturn(Optional.of(verifiedLicense));
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> bookingService.createBooking(userId, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Vehicle is not available for booking");
    }

    @Test
    void should_throw_conflict_when_vehicle_under_maintenance_until_requested_start_date() {
        LocalDate maintenanceEnd = LocalDate.now().plusDays(10);
        vehicle.setStatus(VehicleStatus.MAINTENANCE);
        BookingCreateRequest request = new BookingCreateRequest();
        request.setVehicleId(vehicleId);
        request.setStartDate(maintenanceEnd.minusDays(1));
        request.setEndDate(maintenanceEnd.plusDays(1));
        MaintenanceRecord maintenanceRecord = new MaintenanceRecord();
        maintenanceRecord.setNextDueAt(maintenanceEnd.atTime(9, 0));

        when(bookingRepository.findAllByStatusInAndEndDateBefore(any(), any())).thenReturn(List.of());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(driverLicenseRepository.findByUser_Id(userId)).thenReturn(Optional.of(verifiedLicense));
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(maintenanceRecordRepository.findAllByVehicle_IdOrderByPerformedAtDescCreatedAtDesc(vehicleId))
                .thenReturn(List.of(maintenanceRecord));

        assertThatThrownBy(() -> bookingService.createBooking(userId, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("under maintenance until " + maintenanceEnd.format(DateTimeFormatter.ofPattern("d MMM yyyy")));
    }

    @Test
    void should_create_booking_when_vehicle_maintenance_ends_on_requested_start_date() {
        LocalDate maintenanceEnd = LocalDate.now().plusDays(10);
        vehicle.setStatus(VehicleStatus.MAINTENANCE);
        BookingCreateRequest request = new BookingCreateRequest();
        request.setVehicleId(vehicleId);
        request.setStartDate(maintenanceEnd);
        request.setEndDate(maintenanceEnd.plusDays(2));
        MaintenanceRecord maintenanceRecord = new MaintenanceRecord();
        maintenanceRecord.setNextDueAt(maintenanceEnd.atTime(9, 0));

        Booking saved = new Booking();
        saved.setId(UUID.randomUUID());
        saved.setUserId(userId);
        saved.setVehicleId(vehicleId);
        saved.setPickupLocationId(vehicle.getLocation().getId());
        saved.setDropoffLocationId(vehicle.getLocation().getId());
        saved.setStartDate(request.getStartDate().atStartOfDay());
        saved.setEndDate(request.getEndDate().atTime(java.time.LocalTime.MAX));
        saved.setTotalDays(3);
        saved.setBasePrice(new BigDecimal("300.00"));
        saved.setDiscountAmount(BigDecimal.ZERO);
        saved.setAddonPrice(BigDecimal.ZERO);
        saved.setTotalPrice(new BigDecimal("300.00"));
        saved.setStatus(BookingStatus.PENDING);

        when(bookingRepository.findAllByStatusInAndEndDateBefore(any(), any())).thenReturn(List.of());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(driverLicenseRepository.findByUser_Id(userId)).thenReturn(Optional.of(verifiedLicense));
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(maintenanceRecordRepository.findAllByVehicle_IdOrderByPerformedAtDescCreatedAtDesc(vehicleId))
                .thenReturn(List.of(maintenanceRecord));
        when(bookingRepository.findAllByVehicleIdAndStatusInAndStartDateLessThanAndEndDateGreaterThanOrderByEndDateAsc(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(bookingRepository.save(any(Booking.class))).thenReturn(saved);
        when(vehicleImageRepository.findByVehicleIdOrderByUploadedAtDesc(vehicleId)).thenReturn(List.of());

        BookingResponse result = bookingService.createBooking(userId, request);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.PENDING);
    }

    @Test
    void should_throw_conflict_when_booking_dates_overlap() {
        BookingCreateRequest request = new BookingCreateRequest();
        request.setVehicleId(vehicleId);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(3));
        Booking conflict = new Booking();
        conflict.setId(UUID.randomUUID());
        conflict.setEndDate(LocalDateTime.now().plusDays(4));

        when(bookingRepository.findAllByStatusInAndEndDateBefore(any(), any())).thenReturn(List.of());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(driverLicenseRepository.findByUser_Id(userId)).thenReturn(Optional.of(verifiedLicense));
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(maintenanceRecordRepository.findAllByVehicle_IdOrderByPerformedAtDescCreatedAtDesc(vehicleId)).thenReturn(List.of());
        when(bookingRepository.findAllByVehicleIdAndStatusInAndStartDateLessThanAndEndDateGreaterThanOrderByEndDateAsc(any(), any(), any(), any())).thenReturn(List.of(conflict));

        assertThatThrownBy(() -> bookingService.createBooking(userId, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already reserved for the selected dates");
    }

    @Test
    void should_confirm_booking_when_status_pending() {
        Booking booking = new Booking();
        booking.setId(UUID.randomUUID());
        booking.setUserId(userId);
        booking.setVehicleId(vehicleId);
        booking.setStatus(BookingStatus.PENDING);
        booking.setBasePrice(new BigDecimal("100.00"));
        booking.setAddonPrice(BigDecimal.ZERO);
        booking.setDiscountAmount(BigDecimal.ZERO);
        booking.setTotalPrice(new BigDecimal("100.00"));
        booking.setStartDate(LocalDate.now().plusDays(2).atStartOfDay());
        booking.setEndDate(LocalDate.now().plusDays(4).atTime(java.time.LocalTime.MAX));

        when(bookingRepository.findAllByStatusInAndEndDateBefore(any(), any())).thenReturn(List.of());
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);
        when(paymentRepository.findAllByBookingIdOrderByCreatedAtDesc(booking.getId())).thenReturn(List.of());
        when(bookingRepository.findAllByVehicleIdAndStatusInAndStartDateLessThanAndEndDateGreaterThanOrderByEndDateAsc(
                vehicleId,
                java.util.EnumSet.of(BookingStatus.CONFIRMED, BookingStatus.ACTIVE),
                booking.getEndDate(),
                booking.getStartDate()
        )).thenReturn(List.of());
        when(maintenanceRecordRepository.findAllByVehicle_IdOrderByPerformedAtDescCreatedAtDesc(vehicleId)).thenReturn(List.of());
        when(bookingRepository.findAllByVehicleIdAndStatusInOrderByEndDateAsc(vehicleId, java.util.EnumSet.of(BookingStatus.CONFIRMED, BookingStatus.ACTIVE))).thenReturn(List.of());
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(vehicleRepository.findAllById(any())).thenReturn(List.of(vehicle));
        when(userRepository.findAllById(any())).thenReturn(List.of());
        when(bookingAddonRepository.findByBookingIdIn(any())).thenReturn(List.of());
        when(vehicleImageRepository.findByVehicleIdInOrderByUploadedAtDesc(any())).thenReturn(List.of());

        var result = bookingService.confirmBooking(booking.getId(), new BookingAdminDecisionRequest());

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void should_cancel_booking_for_owner_when_pending() {
        Booking booking = new Booking();
        booking.setId(UUID.randomUUID());
        booking.setUserId(userId);
        booking.setVehicleId(vehicleId);
        booking.setStatus(BookingStatus.PENDING);

        Payment pendingPayment = new Payment();
        pendingPayment.setId(UUID.randomUUID());
        pendingPayment.setBookingId(booking.getId());
        pendingPayment.setStatus(PaymentStatus.PENDING);

        when(bookingRepository.findAllByStatusInAndEndDateBefore(any(), any())).thenReturn(List.of());
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(paymentRepository.findAllByBookingIdOrderByCreatedAtDesc(booking.getId())).thenReturn(List.of(pendingPayment));
        when(bookingRepository.save(booking)).thenReturn(booking);
        when(maintenanceRecordRepository.findAllByVehicle_IdOrderByPerformedAtDescCreatedAtDesc(vehicleId)).thenReturn(List.of());
        when(bookingRepository.findAllByVehicleIdAndStatusInOrderByEndDateAsc(vehicleId, java.util.EnumSet.of(BookingStatus.CONFIRMED, BookingStatus.ACTIVE))).thenReturn(List.of());
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(vehicleRepository.findAllById(any())).thenReturn(List.of(vehicle));
        when(userRepository.findAllById(any())).thenReturn(List.of(user));
        when(bookingAddonRepository.findByBookingIdIn(any())).thenReturn(List.of());
        when(vehicleImageRepository.findByVehicleIdInOrderByUploadedAtDesc(any())).thenReturn(List.of());

        BookingResponse result = bookingService.cancelBooking(userId, booking.getId());

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(pendingPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentRepository).saveAll(any());
    }
}
