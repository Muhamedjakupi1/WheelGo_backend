package com.wheelGo.service;

import com.wheelGo.model.addon.Addon;
import com.wheelGo.model.booking_addons.BookingAddon;
import com.wheelGo.model.bookings.BookingAdminDecisionRequest;
import com.wheelGo.model.bookings.Booking;
import com.wheelGo.model.bookings.BookingCreateRequest;
import com.wheelGo.model.bookings.BookingResponse;
import com.wheelGo.model.enums.AddonType;
import com.wheelGo.model.enums.BookingStatus;
import com.wheelGo.model.enums.VehicleStatus;
import com.wheelGo.model.locations.Location;
import com.wheelGo.model.tenant.Tenant;
import com.wheelGo.model.user.User;
import com.wheelGo.model.vehicle_images.VehicleImage;
import com.wheelGo.model.vehicles.Vehicle;
import com.wheelGo.repository.AddonRepository;
import com.wheelGo.repository.BookingAddonRepository;
import com.wheelGo.repository.BookingRepository;
import com.wheelGo.repository.TenantRepository;
import com.wheelGo.repository.UserRepository;
import com.wheelGo.repository.VehicleImageRepository;
import com.wheelGo.repository.VehicleRepository;
import com.wheelGo.schema.TenantSchemaExecutor;
import lombok.RequiredArgsConstructor;
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
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {
    private static final String BABY_SEAT_NAME = AddonAdminService.BABY_SEAT_NAME;
    private static final String BLUETOOTH_NAME = AddonAdminService.BLUETOOTH_NAME;
    private static final String CUSTOM_ADDON_NAME = "Approved custom request";
    private static final BigDecimal FALLBACK_BABY_SEAT_PRICE = new BigDecimal("25.00");
    private static final BigDecimal FALLBACK_BLUETOOTH_PRICE = new BigDecimal("10.00");
    private static final EnumSet<BookingStatus> BLOCKING_STATUSES =
            EnumSet.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.ACTIVE);
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

        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
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

        boolean overlappingBookingExists =
                bookingRepository.existsByVehicleIdAndStatusInAndStartDateLessThanAndEndDateGreaterThan(
                        vehicle.getId(),
                        BLOCKING_STATUSES,
                        endDateTime,
                        startDateTime
                );

        if (overlappingBookingExists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vehicle is already booked for the selected dates");
        }

        int totalDays = calculateTotalDays(startDate, endDate);
        BigDecimal basePrice = vehicle.getDailyRate()
                .multiply(BigDecimal.valueOf(totalDays))
                .setScale(2, RoundingMode.HALF_UP);

        int babySeatQuantity = resolveBabySeatQuantity(request);
        int bluetoothQuantity = normalizeQuantity(request.getBluetoothQuantity());
        boolean babySeatRequested = babySeatQuantity > 0;
        BigDecimal addonPrice = BigDecimal.ZERO;
        Addon babySeatAddon = findOrCreateManagedAddon(
                BABY_SEAT_NAME,
                "Child safety seat add-on for bookings",
                FALLBACK_BABY_SEAT_PRICE
        );
        Addon bluetoothAddon = findOrCreateManagedAddon(
                BLUETOOTH_NAME,
                "Portable Bluetooth add-on for bookings",
                FALLBACK_BLUETOOTH_PRICE
        );

        validateAndReserve(babySeatAddon, babySeatQuantity);
        validateAndReserve(bluetoothAddon, bluetoothQuantity);
        addonPrice = addonPrice.add(calculateAddonTotal(babySeatAddon, babySeatQuantity));
        addonPrice = addonPrice.add(calculateAddonTotal(bluetoothAddon, bluetoothQuantity));

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

        List<BookingAddon> savedAddons = List.of();
        if (babySeatQuantity > 0 && bluetoothQuantity > 0) {
            savedAddons = List.of(
                    saveBookingAddon(savedBooking.getId(), babySeatAddon, babySeatQuantity),
                    saveBookingAddon(savedBooking.getId(), bluetoothAddon, bluetoothQuantity)
            );
        } else if (babySeatQuantity > 0) {
            savedAddons = List.of(saveBookingAddon(savedBooking.getId(), babySeatAddon, babySeatQuantity));
        } else if (bluetoothQuantity > 0) {
            savedAddons = List.of(saveBookingAddon(savedBooking.getId(), bluetoothAddon, bluetoothQuantity));
        }

        BookingResponse response = toResponse(savedBooking, vehicle, savedAddons, babySeatRequested);
        response.setVehicleImageUrl(
                vehicleImageRepository.findByVehicleIdOrderByUploadedAtDesc(vehicle.getId()).stream()
                        .findFirst()
                        .map(VehicleImage::getUrl)
                        .orElse(null)
        );
        return response;
    }

    @Transactional
    public List<BookingResponse> getBookingsForUser(UUID userId) {
        releaseFinishedAddonInventory();
        return toResponses(bookingRepository.findAllByUserIdOrderByCreatedAtDesc(userId));
    }

    @Transactional
    public List<BookingResponse> getBookingsForAdmin() {
        releaseFinishedAddonInventory();
        return toResponses(bookingRepository.findAllByOrderByCreatedAtDesc());
    }

    @Transactional
    public BookingResponse confirmBooking(UUID bookingId, BookingAdminDecisionRequest request) {
        releaseFinishedAddonInventory();
        Booking booking = findBooking(bookingId);
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending bookings can be confirmed");
        }

        BigDecimal approvedCharge = normalizeMoney(request != null ? request.getAddonCharge() : null);
        if (approvedCharge.compareTo(BigDecimal.ZERO) > 0) {
            Addon customAddon = createCustomAddon(request, booking.getNotes(), approvedCharge);
            saveBookingAddon(booking.getId(), customAddon, 1);

            BigDecimal addonPrice = normalizeMoney(booking.getAddonPrice()).add(approvedCharge);
            booking.setAddonPrice(addonPrice);
            booking.setTotalPrice(
                    normalizeMoney(booking.getBasePrice())
                            .add(addonPrice)
                            .subtract(normalizeMoney(booking.getDiscountAmount()))
                            .setScale(2, RoundingMode.HALF_UP)
            );
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setUpdatedAt(LocalDateTime.now());
        return toResponseWithDetails(bookingRepository.save(booking));
    }

    @Transactional
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
        return toResponseWithDetails(bookingRepository.save(booking));
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
                            .filter(java.util.Objects::nonNull)
                            .toList();
                    boolean babySeatRequested = bookingAddons.stream()
                            .map(bookingAddon -> addonById.get(bookingAddon.getAddonId()))
                            .filter(java.util.Objects::nonNull)
                            .anyMatch(addon -> BABY_SEAT_NAME.equalsIgnoreCase(addon.getName()));

                    BookingResponse response = toResponse(booking, vehicle, bookingAddons, babySeatRequested);
                    response.setVehicleImageUrl(primaryImageByVehicleId.get(booking.getVehicleId()));
                    response.setAddonNames(addonNames);
                    response.setCustomerEmail(user != null ? user.getEmail() : null);
                    return response;
                })
                .toList();
    }

    private BookingAddon saveBookingAddon(UUID bookingId, Addon addon, int quantity) {
        BookingAddon bookingAddon = new BookingAddon();
        bookingAddon.setBookingId(bookingId);
        bookingAddon.setAddonId(addon.getId());
        bookingAddon.setQuantity((short) quantity);
        bookingAddon.setPriceSnapshot(resolvePrice(addon, FALLBACK_BABY_SEAT_PRICE));
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
                    addon.setCreatedAt(LocalDateTime.now());
                    addon.setUpdatedAt(LocalDateTime.now());
                    return addonRepository.save(addon);
                });
    }

    private Addon createCustomAddon(BookingAdminDecisionRequest request, String bookingNotes, BigDecimal price) {
        Addon addon = new Addon();
        addon.setName(resolveCustomAddonName(request));
        addon.setDescription(resolveCustomAddonDescription(request, bookingNotes));
        addon.setPrice(price);
        addon.setQuantity(0);
        addon.setType(AddonType.ONE_TIME);
        addon.setIsActive(false);
        addon.setCreatedAt(LocalDateTime.now());
        addon.setUpdatedAt(LocalDateTime.now());
        return addonRepository.save(addon);
    }

    private String resolveCustomAddonName(BookingAdminDecisionRequest request) {
        String name = normalizeOptionalText(request != null ? request.getAddonName() : null);
        return name != null ? name : CUSTOM_ADDON_NAME;
    }

    private String resolveCustomAddonDescription(BookingAdminDecisionRequest request, String bookingNotes) {
        String note = normalizeOptionalText(request != null ? request.getNote() : null);
        if (note != null) {
            return note;
        }
        return normalizeOptionalText(bookingNotes);
    }

    private BigDecimal resolvePrice(Addon addon, BigDecimal fallback) {
        BigDecimal price = addon != null && addon.getPrice() != null ? addon.getPrice() : fallback;
        return normalizeMoney(price);
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return (value != null ? value : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateAddonTotal(Addon addon, int quantity) {
        if (quantity <= 0) {
            return BigDecimal.ZERO;
        }
        return resolvePrice(addon, BigDecimal.ZERO).multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    private void validateAndReserve(Addon addon, int quantity) {
        if (quantity <= 0) {
            return;
        }
        if (addon.getQuantity() < quantity) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    addon.getName() + " has only " + addon.getQuantity() + " available"
            );
        }
        addon.setQuantity(addon.getQuantity() - quantity);
        addon.setUpdatedAt(LocalDateTime.now());
        addonRepository.save(addon);
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
            if (addon == null || !isManagedInventoryAddon(addon)) {
                continue;
            }
            addon.setQuantity(addon.getQuantity() + bookingAddon.getQuantity());
            addon.setUpdatedAt(LocalDateTime.now());
            addonRepository.save(addon);
        }
    }

    private boolean isManagedInventoryAddon(Addon addon) {
        return BABY_SEAT_NAME.equalsIgnoreCase(addon.getName()) || BLUETOOTH_NAME.equalsIgnoreCase(addon.getName());
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

    private int calculateTotalDays(LocalDate startDate, LocalDate endDate) {
        long diff = ChronoUnit.DAYS.between(startDate, endDate);
        return (int) Math.max(diff, 1);
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
}
