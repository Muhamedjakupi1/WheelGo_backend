package com.wheelGo.service;

import com.wheelGo.config.CacheNames;
import com.wheelGo.model.addon.Addon;
import com.wheelGo.model.booking_addons.BookingAddon;
import com.wheelGo.model.bookings.BookingAdminDecisionRequest;
import com.wheelGo.model.bookings.BookingAdminUpdateRequest;
import com.wheelGo.model.bookings.Booking;
import com.wheelGo.model.bookings.BookingCreateRequest;
import com.wheelGo.model.bookings.BookingResponse;
import com.wheelGo.model.bookings.BookingSelectedAddonRequest;
import com.wheelGo.model.enums.AddonType;
import com.wheelGo.model.enums.BookingStatus;
import com.wheelGo.model.enums.VehicleStatus;
import com.wheelGo.model.maintenance_records.MaintenanceRecord;
import com.wheelGo.model.payments.Payment;
import com.wheelGo.model.locations.Location;
import com.wheelGo.model.invoices.Invoice;
import com.wheelGo.model.tenant.Tenant;
import com.wheelGo.model.user.User;
import com.wheelGo.model.vehicle_images.VehicleImage;
import com.wheelGo.model.vehicles.Vehicle;
import com.wheelGo.repository.AddonRepository;
import com.wheelGo.repository.BookingAddonRepository;
import com.wheelGo.repository.BookingRepository;
import com.wheelGo.repository.MaintenanceRecordRepository;
import com.wheelGo.repository.PaymentRepository;
import com.wheelGo.repository.InvoiceRepository;
import com.wheelGo.repository.TenantRepository;
import com.wheelGo.repository.UserRepository;
import com.wheelGo.repository.VehicleImageRepository;
import com.wheelGo.repository.VehicleRepository;
import com.wheelGo.schema.TenantSchemaExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {
    private static final String BABY_SEAT_NAME = AddonAdminService.BABY_SEAT_NAME;
    private static final String BLUETOOTH_NAME = AddonAdminService.BLUETOOTH_NAME;
    private static final BigDecimal FALLBACK_BABY_SEAT_PRICE = new BigDecimal("25.00");
    private static final BigDecimal FALLBACK_BLUETOOTH_PRICE = new BigDecimal("10.00");
    private static final EnumSet<BookingStatus> BLOCKING_STATUSES =
            EnumSet.of(BookingStatus.CONFIRMED, BookingStatus.ACTIVE);
    private static final EnumSet<BookingStatus> RELEASABLE_STATUSES =
            EnumSet.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.ACTIVE);

    private final BookingRepository bookingRepository;
    private final BookingAddonRepository bookingAddonRepository;
    private final AddonRepository addonRepository;
    private final TenantRepository tenantRepository;
    private final TenantSchemaExecutor tenantSchemaExecutor;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleImageRepository vehicleImageRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final CacheInvalidationService cacheInvalidationService;
    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;

    @Transactional
    public BookingResponse createBooking(UUID userId, BookingCreateRequest request) {
        releaseFinishedAddonInventory();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found"));

        if (vehicle.getLocation() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vehicle location is not configured");
        }

        if (vehicle.getStatus() == VehicleStatus.INACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vehicle is not available for booking");
        }

        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();

        if (startDate == null || endDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking dates are required");
        }

        if (endDate.isBefore(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date cannot be before start date");
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        ensureVehicleDatesAvailable(vehicle.getId(), startDateTime, endDateTime, null);

        int totalDays = calculateTotalDays(startDate, endDate);
        BigDecimal basePrice = vehicle.getDailyRate()
                .multiply(BigDecimal.valueOf(totalDays))
                .setScale(2, RoundingMode.HALF_UP);

        List<SelectedAddon> selectedAddons = resolveRequestedAddons(request);
        boolean babySeatRequested = selectedAddons.stream()
                .map(SelectedAddon::addon)
                .filter(Objects::nonNull)
                .anyMatch(addon -> BABY_SEAT_NAME.equalsIgnoreCase(addon.getName()));
        BigDecimal addonPrice = BigDecimal.ZERO;
        for (SelectedAddon selectedAddon : selectedAddons) {
            validateAndReserve(selectedAddon.addon(), selectedAddon.quantity());
            addonPrice = addonPrice.add(calculateAddonTotal(selectedAddon.addon(), selectedAddon.quantity(), totalDays));
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal totalPrice = basePrice.add(addonPrice).subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);

        Booking booking = new Booking();
        booking.setUserId(user.getId());
        booking.setVehicleId(vehicle.getId());
        booking.setPickupLocationId(vehicle.getLocation().getId());
        booking.setDropoffLocationId(vehicle.getLocation().getId());
        booking.setStartDate(startDateTime);
        booking.setEndDate(endDateTime);
        booking.setTotalDays(totalDays);
        booking.setBasePrice(basePrice);
        booking.setDiscountAmount(discountAmount);
        booking.setAddonPrice(addonPrice);
        booking.setTotalPrice(totalPrice);
        booking.setStatus(BookingStatus.PENDING);
        booking.setNotes(normalizeOptionalText(request.getSpecialRequest()));
        booking.setCreatedAt(LocalDateTime.now());
        booking.setUpdatedAt(LocalDateTime.now());

        Booking savedBooking = bookingRepository.save(booking);

        List<BookingAddon> savedAddons = selectedAddons.stream()
                .map(selectedAddon -> saveBookingAddon(savedBooking.getId(), selectedAddon.addon(), selectedAddon.quantity(), totalDays))
                .toList();

        BookingResponse response = toResponse(savedBooking, vehicle, savedAddons, babySeatRequested);
        response.setVehicleImageUrl(
                vehicleImageRepository.findByVehicleIdOrderByUploadedAtDesc(vehicle.getId()).stream()
                        .findFirst()
                        .map(VehicleImage::getUrl)
                        .orElse(null)
        );
        cacheInvalidationService.evictBookings(userId);
        cacheInvalidationService.evictBookingsForAdmin(); // Pastron listën e përgjithshme të adminit
        cacheInvalidationService.evictVehicle(savedBooking.getVehicleId());
        return response;
    }


    @Transactional
    @Cacheable(value = CacheNames.BOOKINGS, key = "'user:' + #userId")
    public List<BookingResponse> getBookingsForUser(UUID userId) {
        releaseFinishedAddonInventory();
        return toResponses(bookingRepository.findAllByUserIdOrderByCreatedAtDesc(userId));
    }

    @Transactional
    public List<BookingResponse> getBookingsForUser(UUID userId, String keyword) {
        List<BookingResponse> allBookings = getBookingsForUser(userId);

        if (keyword == null || keyword.trim().isEmpty()) {
            return allBookings;
        }

        String lowerKeyword = keyword.trim().toLowerCase();

        return allBookings.stream()
                .filter(b -> {
                    boolean matchesVehicle = b.getVehicleName() != null && b.getVehicleName().toLowerCase().contains(lowerKeyword);
                    boolean matchesLocation = b.getLocationName() != null && b.getLocationName().toLowerCase().contains(lowerKeyword);
                    boolean matchesStatus = b.getStatus() != null && b.getStatus().name().toLowerCase().contains(lowerKeyword);
                    boolean matchesNotes = b.getNotes() != null && b.getNotes().toLowerCase().contains(lowerKeyword);


                    Vehicle vehicle = vehicleRepository.findById(b.getVehicleId()).orElse(null);
                    boolean matchesTransmission = false;
                    if (vehicle != null && vehicle.getTransmission() != null) {
                        matchesTransmission = vehicle.getTransmission().name().toLowerCase().contains(lowerKeyword);
                    }

                    return matchesVehicle || matchesLocation || matchesStatus || matchesNotes || matchesTransmission;
                })
                .toList();
    }


    @Transactional
    @Cacheable(value = CacheNames.BOOKINGS, key = "'admin:all'")
    public List<BookingResponse> getBookingsForAdmin() {
        releaseFinishedAddonInventory();
        return toResponses(bookingRepository.findAllByOrderByCreatedAtDesc());
    }

    @Transactional
    public List<BookingResponse> getBookingsForAdmin(String keyword) {
        List<BookingResponse> allBookings = getBookingsForAdmin();

        if (keyword == null || keyword.trim().isEmpty()) {
            return allBookings;
        }

        String lowerKeyword = keyword.trim().toLowerCase();

        return allBookings.stream()
                .filter(b -> {
                    boolean matchesVehicle = b.getVehicleName() != null && b.getVehicleName().toLowerCase().contains(lowerKeyword);
                    boolean matchesEmail = b.getCustomerEmail() != null && b.getCustomerEmail().toLowerCase().contains(lowerKeyword);
                    boolean matchesLocation = b.getLocationName() != null && b.getLocationName().toLowerCase().contains(lowerKeyword);
                    boolean matchesStatus = b.getStatus() != null && b.getStatus().name().toLowerCase().contains(lowerKeyword);
                    boolean matchesNotes = b.getNotes() != null && b.getNotes().toLowerCase().contains(lowerKeyword);

                    Vehicle vehicle = vehicleRepository.findById(b.getVehicleId()).orElse(null);
                    boolean matchesTransmission = false;
                    if (vehicle != null && vehicle.getTransmission() != null) {
                        matchesTransmission = vehicle.getTransmission().name().toLowerCase().contains(lowerKeyword);
                    }

                    return matchesVehicle || matchesEmail || matchesLocation || matchesStatus || matchesNotes || matchesTransmission;
                })
                .toList();
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.BOOKINGS, key = "'user:' + #result.userId"),
            @CacheEvict(value = CacheNames.BOOKINGS, key = "'admin:all'")
    })
    public BookingResponse confirmBooking(UUID bookingId, BookingAdminDecisionRequest request) {
        releaseFinishedAddonInventory();
        Booking booking = findBooking(bookingId);
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending bookings can be confirmed");
        }

        BigDecimal approvedCharge = normalizeMoney(request != null ? request.getAddonCharge() : null);
        if (approvedCharge.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal addonPrice = normalizeMoney(booking.getAddonPrice()).add(approvedCharge);
            booking.setAddonPrice(addonPrice);
            booking.setTotalPrice(
                    normalizeMoney(booking.getBasePrice())
                            .add(addonPrice)
                            .subtract(normalizeMoney(booking.getDiscountAmount()))
                            .setScale(2, RoundingMode.HALF_UP)
            );
            appendApprovedChargeNote(booking, request != null ? request.getAddonName() : null, approvedCharge);
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setUpdatedAt(LocalDateTime.now());
        Booking savedBooking = bookingRepository.save(booking);
        syncVehicleStatus(savedBooking.getVehicleId());
        cacheInvalidationService.evictVehicle(savedBooking.getVehicleId());
        return toResponseWithDetails(savedBooking);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.BOOKINGS, key = "'user:' + #result.userId"),
            @CacheEvict(value = CacheNames.BOOKINGS, key = "'admin:all'")
    })
    public BookingResponse rejectBooking(UUID bookingId, BookingAdminDecisionRequest request) {
        releaseFinishedAddonInventory();
        Booking booking = findBooking(bookingId);
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending bookings can be rejected");
        }

        String note = normalizeOptionalText(request != null ? request.getNote() : null);
        if (note != null) {
            booking.setNotes(appendNote(booking.getNotes(), "Admin rejection note: " + note));
        }
        releaseBookingAddonInventory(booking);
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setUpdatedAt(LocalDateTime.now());
        Booking savedBooking = bookingRepository.save(booking);
        syncVehicleStatus(savedBooking.getVehicleId());
        cacheInvalidationService.evictVehicle(savedBooking.getVehicleId());
        return toResponseWithDetails(savedBooking);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.BOOKINGS, key = "'user:' + #result.userId"),
            @CacheEvict(value = CacheNames.BOOKINGS, key = "'admin:all'")
    })
    public BookingResponse updateBookingAsAdmin(UUID bookingId, BookingAdminUpdateRequest request) {
        releaseFinishedAddonInventory();
        Booking booking = findBooking(bookingId);
        Vehicle vehicle = vehicleRepository.findById(booking.getVehicleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found"));

        LocalDateTime startDateTime = booking.getStartDate();
        LocalDateTime endDateTime = booking.getEndDate();

        BookingStatus targetStatus = request.getStatus() != null ? request.getStatus() : booking.getStatus();

        if (BLOCKING_STATUSES.contains(targetStatus)) {
            ensureVehicleDatesAvailable(vehicle.getId(), startDateTime, endDateTime, booking.getId());
        }

        boolean wasInventoryReserved = RELEASABLE_STATUSES.contains(booking.getStatus());
        boolean shouldReleaseInventory = wasInventoryReserved && isTerminalStatus(targetStatus);

        if (request.getAddonCharge() != null && request.getAddonCharge().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal addonPrice = normalizeMoney(booking.getAddonPrice()).add(normalizeMoney(request.getAddonCharge()));
            booking.setAddonPrice(addonPrice);
            booking.setTotalPrice(
                    normalizeMoney(booking.getBasePrice())
                            .add(addonPrice)
                            .subtract(normalizeMoney(booking.getDiscountAmount()))
                            .setScale(2, RoundingMode.HALF_UP)
            );
            appendApprovedChargeNote(booking, request.getAddonName(), normalizeMoney(request.getAddonCharge()));
        }

        String note = normalizeOptionalText(request.getNote());
        if (note != null) {
            booking.setNotes(appendNote(booking.getNotes(), "Admin note: " + note));
        }

        if (shouldReleaseInventory) {
            releaseBookingAddonInventory(booking);
        }

        booking.setStatus(targetStatus);
        booking.setUpdatedAt(LocalDateTime.now());
        Booking savedBooking = bookingRepository.save(booking);
        syncVehicleStatus(savedBooking.getVehicleId());
        cacheInvalidationService.evictVehicle(savedBooking.getVehicleId());
        return toResponseWithDetails(savedBooking);
    }

    @Transactional
    public void deleteBookingAsAdmin(UUID bookingId) {
        releaseFinishedAddonInventory();
        Booking booking = findBooking(bookingId);
        if (RELEASABLE_STATUSES.contains(booking.getStatus())) {
            releaseBookingAddonInventory(booking);
        }
        UUID vehicleId = booking.getVehicleId();
        UUID userId = booking.getUserId();
        bookingRepository.delete(booking);
        syncVehicleStatus(vehicleId);
        cacheInvalidationService.evictBookings(userId);
        cacheInvalidationService.evictBookingsForAdmin();
        cacheInvalidationService.evictVehicle(vehicleId);
    }

    private List<BookingResponse> toResponses(List<Booking> bookings) {
        if (bookings.isEmpty()) {
            return List.of();
        }

        List<UUID> vehicleIds = bookings.stream().map(Booking::getVehicleId).distinct().toList();
        Map<UUID, Vehicle> vehiclesById = vehicleRepository.findAllById(vehicleIds).stream()
                .collect(Collectors.toMap(Vehicle::getId, Function.identity()));

        List<UUID> userIds = bookings.stream().map(Booking::getUserId).distinct().toList();
        Map<UUID, User> usersById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<UUID> bookingIds = bookings.stream().map(Booking::getId).toList();
        Map<UUID, List<BookingAddon>> addonsByBookingId = bookingAddonRepository.findByBookingIdIn(bookingIds).stream()
                .collect(Collectors.groupingBy(BookingAddon::getBookingId));

        Map<UUID, List<Payment>> paymentsByBookingId = paymentRepository == null
                ? Collections.emptyMap()
                : paymentRepository.findAllByBookingIdInOrderByCreatedAtDesc(bookingIds).stream()
                .collect(Collectors.groupingBy(Payment::getBookingId));

        Map<UUID, Payment> latestPaymentByBookingId = paymentsByBookingId.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toMap(
                        Payment::getBookingId,
                        Function.identity(),
                        (existing, ignored) -> existing
                ));

        Map<UUID, Invoice> invoiceByBookingId = invoiceRepository == null
                ? Collections.emptyMap()
                : invoiceRepository.findByBookingIdIn(bookingIds).stream()
                .collect(Collectors.toMap(Invoice::getBookingId, Function.identity()));

        List<UUID> addonIds = addonsByBookingId.values().stream()
                .flatMap(List::stream)
                .map(BookingAddon::getAddonId)
                .distinct()
                .toList();
        Map<UUID, Addon> addonById = addonIds.isEmpty()
                ? Collections.emptyMap()
                : addonRepository.findAllById(addonIds).stream()
                .collect(Collectors.toMap(Addon::getId, Function.identity()));

        Map<UUID, String> primaryImageByVehicleId = vehicleIds.isEmpty()
                ? Collections.emptyMap()
                : vehicleImageRepository.findByVehicleIdInOrderByUploadedAtDesc(vehicleIds).stream()
                .collect(Collectors.groupingBy(image -> image.getVehicle().getId()))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> pickPrimaryImageUrl(entry.getValue())
                ));

        return bookings.stream()
                .map(booking -> {
                    Vehicle vehicle = vehiclesById.get(booking.getVehicleId());
                    User user = usersById.get(booking.getUserId());
                    List<BookingAddon> bookingAddons = addonsByBookingId.getOrDefault(booking.getId(), List.of());
                    List<String> addonNames = bookingAddons.stream()
                            .map(bookingAddon -> formatAddonName(bookingAddon, addonById.get(bookingAddon.getAddonId())))
                            .filter(Objects::nonNull)
                            .toList();
                    boolean babySeatRequested = bookingAddons.stream()
                            .map(bookingAddon -> addonById.get(bookingAddon.getAddonId()))
                            .filter(Objects::nonNull)
                            .anyMatch(addon -> BABY_SEAT_NAME.equalsIgnoreCase(addon.getName()));

                    BookingResponse response = toResponse(booking, vehicle, bookingAddons, babySeatRequested);
                    Payment latestPayment = latestPaymentByBookingId.get(booking.getId());
                    Invoice invoice = invoiceByBookingId.get(booking.getId());
                    response.setVehicleImageUrl(primaryImageByVehicleId.get(booking.getVehicleId()));
                    response.setAddonNames(addonNames);
                    response.setCustomerEmail(user != null ? user.getEmail() : null);
                    if (latestPayment != null) {
                        response.setPaymentStatus(latestPayment.getStatus());
                        response.setPaymentMethod(latestPayment.getMethod());
                    }
                    response.setInvoiceNumber(invoice != null ? invoice.getInvoiceNumber() : null);
                    applyPaymentSummary(response, latestPayment, paymentsByBookingId.getOrDefault(booking.getId(), List.of()));
                    return response;
                })
                .toList();
    }

    private BookingAddon saveBookingAddon(UUID bookingId, Addon addon, int quantity, int totalDays) {
        BookingAddon bookingAddon = new BookingAddon();
        bookingAddon.setBookingId(bookingId);
        bookingAddon.setAddonId(addon.getId());
        bookingAddon.setQuantity((short) quantity);
        bookingAddon.setPriceSnapshot(resolveSnapshotPrice(addon, totalDays));
        bookingAddon.setCreatedAt(LocalDateTime.now());
        return bookingAddonRepository.save(bookingAddon);
    }

    private BookingResponse toResponse(Booking booking, Vehicle vehicle, List<BookingAddon> bookingAddons, boolean babySeatRequested) {
        BookingResponse response = new BookingResponse();
        response.setId(booking.getId());
        response.setUserId(booking.getUserId());
        response.setVehicleId(booking.getVehicleId());
        response.setPickupLocationId(booking.getPickupLocationId());
        response.setDropoffLocationId(booking.getDropoffLocationId());
        response.setPromotionId(booking.getPromotionId());
        response.setStartDate(booking.getStartDate());
        response.setEndDate(booking.getEndDate());
        response.setTotalDays(booking.getTotalDays());
        response.setBasePrice(booking.getBasePrice());
        response.setDiscountAmount(booking.getDiscountAmount());
        response.setAddonPrice(booking.getAddonPrice());
        response.setTotalPrice(booking.getTotalPrice());
        response.setStatus(booking.getStatus());
        response.setNotes(booking.getNotes());
        response.setSpecialRequest(booking.getNotes());
        response.setCreatedAt(booking.getCreatedAt());
        response.setUpdatedAt(booking.getUpdatedAt());
        response.setBabySeatRequested(babySeatRequested);

        if (vehicle != null) {
            response.setVehicleName(vehicle.getMake() + " " + vehicle.getModel());
            response.setLocationName(readLocationName(vehicle.getLocation()));
        }

        if (!bookingAddons.isEmpty() && (response.getAddonNames() == null || response.getAddonNames().isEmpty())) {
            response.setAddonNames(bookingAddons.stream().map(BookingAddon::getAddonId).map(UUID::toString).toList());
        }

        return response;
    }

    private void applyPaymentSummary(BookingResponse response, Payment latestPayment, List<Payment> bookingPayments) {
        BigDecimal totalPrice = normalizeMoney(response.getTotalPrice());
        BigDecimal paidAmount = bookingPayments.stream()
                .filter(payment -> payment.getStatus() == com.wheelGo.model.enums.PaymentStatus.PAID)
                .map(Payment::getAmount)
                .map(this::normalizeMoney)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        response.setPaidAmount(paidAmount);
        response.setOutstandingAmount(totalPrice.subtract(paidAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP));
    }

    private BookingResponse toResponseWithDetails(Booking booking) {
        return toResponses(List.of(booking)).getFirst();
    }

    private Booking findBooking(UUID id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
    }

    private Addon findOrCreateManagedAddon(String name, String description, BigDecimal fallbackPrice) {
        return addonRepository.findFirstByNameIgnoreCaseAndIsActiveTrue(name)
                .orElseGet(() -> {
                    Addon addon = new Addon();
                    addon.setName(name);
                    addon.setDescription(description);
                    addon.setPrice(fallbackPrice);
                    addon.setQuantity(0);
                    addon.setType(AddonType.ONE_TIME);
                    addon.setIsActive(true);
                    addon.setInventoryManaged(true);
                    addon.setCreatedAt(LocalDateTime.now());
                    addon.setUpdatedAt(LocalDateTime.now());
                    return addonRepository.save(addon);
                });
    }

    private BigDecimal resolvePrice(Addon addon, BigDecimal fallback) {
        BigDecimal price = addon != null && addon.getPrice() != null ? addon.getPrice() : fallback;
        return normalizeMoney(price);
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return (value != null ? value : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateAddonTotal(Addon addon, int quantity, int totalDays) {
        if (quantity <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = resolvePrice(addon, BigDecimal.ZERO).multiply(BigDecimal.valueOf(quantity));
        if (addon != null && addon.getType() == AddonType.DAILY) {
            total = total.multiply(BigDecimal.valueOf(totalDays));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private void validateAndReserve(Addon addon, int quantity) {
        if (quantity <= 0) {
            return;
        }
        if (Boolean.TRUE.equals(addon.getInventoryManaged()) && addon.getQuantity() < quantity) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    addon.getName() + " has only " + addon.getQuantity() + " available"
            );
        }
        if (Boolean.TRUE.equals(addon.getInventoryManaged())) {
            addon.setQuantity(addon.getQuantity() - quantity);
            addon.setUpdatedAt(LocalDateTime.now());
            addonRepository.save(addon);
        }
    }

    @Scheduled(
            initialDelayString = "${app.bookings.inventory-release-initial-delay-ms:60000}",
            fixedDelayString = "${app.bookings.inventory-release-delay-ms:300000}"
    )
    public void releaseFinishedAddonInventoryForAllTenants() {
        tenantRepository.findAll().stream()
                .filter(tenant -> tenant.getSchemaName() != null && !tenant.getSchemaName().isBlank())
                .forEach(this::releaseFinishedAddonInventoryForTenant);
    }

    private void releaseFinishedAddonInventoryForTenant(Tenant tenant) {
        tenantSchemaExecutor.runForTenant(tenant, this::releaseFinishedAddonInventory);
    }

    @Transactional
    public void releaseFinishedAddonInventory() {
        List<Booking> finishedBookings = bookingRepository.findAllByStatusInAndEndDateBefore(
                RELEASABLE_STATUSES,
                LocalDateTime.now()
        );
        for (Booking booking : finishedBookings) {
            releaseBookingAddonInventory(booking);
            booking.setStatus(booking.getStatus() == BookingStatus.PENDING ? BookingStatus.CANCELLED : BookingStatus.COMPLETED);
            booking.setUpdatedAt(LocalDateTime.now());
        }
        if (!finishedBookings.isEmpty()) {
            bookingRepository.saveAll(finishedBookings);
            finishedBookings.stream()
                    .map(Booking::getVehicleId)
                    .distinct()
                    .forEach(vehicleId -> {
                        syncVehicleStatus(vehicleId);
                        cacheInvalidationService.evictVehicle(vehicleId);
                    });
            finishedBookings.stream()
                    .map(Booking::getUserId)
                    .distinct()
                    .forEach(cacheInvalidationService::evictBookingsForUser);
            cacheInvalidationService.evictBookingsForAdmin();
        }
    }

    private void releaseBookingAddonInventory(Booking booking) {
        List<BookingAddon> bookingAddons = bookingAddonRepository.findByBookingIdIn(List.of(booking.getId()));
        if (bookingAddons.isEmpty()) {
            return;
        }

        Map<UUID, Addon> addonsById = addonRepository.findAllById(
                bookingAddons.stream().map(BookingAddon::getAddonId).distinct().toList()
        ).stream().collect(Collectors.toMap(Addon::getId, Function.identity()));

        for (BookingAddon bookingAddon : bookingAddons) {
            Addon addon = addonsById.get(bookingAddon.getAddonId());
            if (addon == null || !Boolean.TRUE.equals(addon.getInventoryManaged())) {
                continue;
            }
            addon.setQuantity(addon.getQuantity() + bookingAddon.getQuantity());
            addon.setUpdatedAt(LocalDateTime.now());
            addonRepository.save(addon);
        }
    }

    private boolean isTerminalStatus(BookingStatus status) {
        return status == BookingStatus.COMPLETED || status == BookingStatus.CANCELLED;
    }

    private int resolveBabySeatQuantity(BookingCreateRequest request) {
        int requestedQuantity = normalizeQuantity(request.getBabySeatQuantity());
        if (requestedQuantity == 0 && Boolean.TRUE.equals(request.getBabySeatRequested())) {
            return 1;
        }
        return requestedQuantity;
    }

    private int normalizeQuantity(Integer quantity) {
        return quantity != null ? quantity : 0;
    }

    private String formatAddonName(BookingAddon bookingAddon, Addon addon) {
        if (addon == null) {
            return null;
        }
        int quantity = bookingAddon.getQuantity() != null ? bookingAddon.getQuantity() : 1;
        return quantity > 1 ? addon.getName() + " x" + quantity : addon.getName();
    }

    private String appendNote(String existing, String addition) {
        String normalizedExisting = normalizeOptionalText(existing);
        return normalizedExisting == null ? addition : normalizedExisting + "\n\n" + addition;
    }

    private void appendApprovedChargeNote(Booking booking, String addonName, BigDecimal charge) {
        String normalizedName = normalizeOptionalText(addonName);
        String note = normalizedName != null
                ? "Approved special request charge (" + normalizedName + "): +" + charge
                : "Approved special request charge: +" + charge;
        booking.setNotes(appendNote(booking.getNotes(), note));
    }

    private int calculateTotalDays(LocalDate startDate, LocalDate endDate) {
        long diff = ChronoUnit.DAYS.between(startDate, endDate);
        return (int) Math.max(diff + 1, 1);
    }

    private void validateBookingDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking dates are required");
        }

        if (endDate.isBefore(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date cannot be before start date");
        }
    }

    private void ensureVehicleDatesAvailable(UUID vehicleId,
                                             LocalDateTime startDateTime,
                                             LocalDateTime endDateTime,
                                             UUID excludedBookingId) {
        ensureMaintenanceWindowAllowsBooking(vehicleId, startDateTime.toLocalDate());

        List<Booking> conflicts = bookingRepository
                .findAllByVehicleIdAndStatusInAndStartDateLessThanAndEndDateGreaterThanOrderByEndDateAsc(
                        vehicleId,
                        BLOCKING_STATUSES,
                        endDateTime,
                        startDateTime
                ).stream()
                .filter(booking -> excludedBookingId == null || !booking.getId().equals(excludedBookingId))
                .toList();

        if (conflicts.isEmpty()) {
            return;
        }

        LocalDate latestBlockedDate = conflicts.stream()
                .map(Booking::getEndDate)
                .max(LocalDateTime::compareTo)
                .orElse(endDateTime)
                .toLocalDate();

        String formattedDate = latestBlockedDate.format(DateTimeFormatter.ofPattern("d MMM yyyy"));
        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Vehicle is already reserved for the selected dates. It will be free after " + formattedDate + "."
        );
    }

    private void ensureMaintenanceWindowAllowsBooking(UUID vehicleId, LocalDate requestedStartDate) {
        MaintenanceAvailability maintenanceAvailability = resolveMaintenanceAvailability(vehicleId);
        if (!maintenanceAvailability.active()) {
            return;
        }

        LocalDate availableFrom = maintenanceAvailability.availableFrom();
        if (availableFrom != null && !requestedStartDate.isBefore(availableFrom)) {
            return;
        }

        if (availableFrom != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Vehicle is under maintenance until " + availableFrom.format(DateTimeFormatter.ofPattern("d MMM yyyy")) +
                            " and cannot be booked for the selected dates."
            );
        }

        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Vehicle is under maintenance and is not available for booking right now."
        );
    }

    private void syncVehicleStatus(UUID vehicleId) {
        if (vehicleId == null) {
            return;
        }

        Vehicle vehicle = vehicleRepository.findById(vehicleId).orElse(null);
        if (vehicle == null) {
            return;
        }

        if (vehicle.getStatus() == VehicleStatus.INACTIVE) {
            return;
        }

        MaintenanceAvailability maintenanceAvailability = resolveMaintenanceAvailability(vehicleId);
        if (maintenanceAvailability.active()) {
            if (vehicle.getStatus() != VehicleStatus.MAINTENANCE) {
                vehicle.setStatus(VehicleStatus.MAINTENANCE);
                vehicle.setUpdatedAt(LocalDateTime.now());
                vehicleRepository.save(vehicle);
            }
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        Optional<Booking> activeBlockingBooking = bookingRepository
                .findAllByVehicleIdAndStatusInOrderByEndDateAsc(vehicleId, BLOCKING_STATUSES)
                .stream()
                .filter(booking -> !booking.getStartDate().isAfter(now) && !booking.getEndDate().isBefore(now))
                .max((left, right) -> left.getEndDate().compareTo(right.getEndDate()));

        if (activeBlockingBooking.isPresent()) {
            if (vehicle.getStatus() != VehicleStatus.RENTED) {
                vehicle.setStatus(VehicleStatus.RENTED);
                vehicle.setUpdatedAt(LocalDateTime.now());
                vehicleRepository.save(vehicle);
            }
            return;
        }

        if (vehicle.getStatus() == VehicleStatus.RENTED) {
            vehicle.setStatus(VehicleStatus.AVAILABLE);
            vehicle.setUpdatedAt(LocalDateTime.now());
            vehicleRepository.save(vehicle);
        }
    }

    private MaintenanceAvailability resolveMaintenanceAvailability(UUID vehicleId) {
        List<MaintenanceRecord> records = maintenanceRecordRepository.findAllByVehicle_IdOrderByPerformedAtDescCreatedAtDesc(vehicleId);
        if (records.isEmpty()) {
            return new MaintenanceAvailability(false, null);
        }

        if (records.stream().anyMatch(record -> record.getNextDueAt() == null)) {
            return new MaintenanceAvailability(true, null);
        }

        LocalDate availableFrom = records.stream()
                .map(MaintenanceRecord::getNextDueAt)
                .filter(Objects::nonNull)
                .map(LocalDateTime::toLocalDate)
                .max(LocalDate::compareTo)
                .orElse(null);

        boolean active = availableFrom != null && LocalDate.now().isBefore(availableFrom);
        return new MaintenanceAvailability(active, availableFrom);
    }

    private String readLocationName(Location location) {
        return location != null ? location.getName() : null;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String pickPrimaryImageUrl(List<VehicleImage> images) {
        return images.stream()
                .filter(VehicleImage::isPrimary)
                .map(VehicleImage::getUrl)
                .findFirst()
                .orElseGet(() -> images.stream().map(VehicleImage::getUrl).findFirst().orElse(null));
    }

    private BigDecimal resolveSnapshotPrice(Addon addon, int totalDays) {
        BigDecimal basePrice = resolvePrice(addon, BigDecimal.ZERO);
        if (addon != null && addon.getType() == AddonType.DAILY) {
            return basePrice.multiply(BigDecimal.valueOf(totalDays)).setScale(2, RoundingMode.HALF_UP);
        }
        return basePrice;
    }

    private List<SelectedAddon> resolveRequestedAddons(BookingCreateRequest request) {
        Map<UUID, Integer> requestedQuantities = new LinkedHashMap<>();

        for (BookingSelectedAddonRequest addonRequest : request.getAddons()) {
            if (addonRequest.getAddonId() == null || addonRequest.getQuantity() == null || addonRequest.getQuantity() <= 0) {
                continue;
            }
            requestedQuantities.merge(addonRequest.getAddonId(), addonRequest.getQuantity(), Integer::sum);
        }

        int babySeatQuantity = resolveBabySeatQuantity(request);
        if (babySeatQuantity > 0) {
            Addon babySeatAddon = findOrCreateManagedAddon(
                    BABY_SEAT_NAME,
                    "Child safety seat add-on for bookings",
                    FALLBACK_BABY_SEAT_PRICE
            );
            requestedQuantities.merge(babySeatAddon.getId(), babySeatQuantity, Integer::sum);
        }

        int bluetoothQuantity = normalizeQuantity(request.getBluetoothQuantity());
        if (bluetoothQuantity > 0) {
            Addon bluetoothAddon = findOrCreateManagedAddon(
                    BLUETOOTH_NAME,
                    "Portable Bluetooth add-on for bookings",
                    FALLBACK_BLUETOOTH_PRICE
            );
            requestedQuantities.merge(bluetoothAddon.getId(), bluetoothQuantity, Integer::sum);
        }

        if (requestedQuantities.isEmpty()) {
            return List.of();
        }

        Map<UUID, Addon> addonsById = addonRepository.findAllById(requestedQuantities.keySet()).stream()
                .collect(Collectors.toMap(Addon::getId, Function.identity()));

        return requestedQuantities.entrySet().stream()
                .map(entry -> {
                    Addon addon = addonsById.get(entry.getKey());
                    if (addon == null || !Boolean.TRUE.equals(addon.getIsActive()) || Boolean.TRUE.equals(addon.getIsDeleted())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more selected add-ons are no longer available");
                    }
                    return new SelectedAddon(addon, entry.getValue());
                })
                .toList();
    }

    private record MaintenanceAvailability(boolean active, LocalDate availableFrom) {}
    private record SelectedAddon(Addon addon, int quantity) {}
}