package com.wheelGo.service;

import com.wheelGo.mapper.PaymentMapper;
import com.wheelGo.model.bookings.Booking;
import com.wheelGo.model.enums.BookingStatus;
import com.wheelGo.model.enums.PaymentMethod;
import com.wheelGo.model.enums.PaymentStatus;
import com.wheelGo.model.invoices.Invoice;
import com.wheelGo.model.invoices.InvoiceEmailRequest;
import com.wheelGo.model.payments.Payment;
import com.wheelGo.model.payments.PaymentAdminUpdateRequest;
import com.wheelGo.model.payments.PaymentRequest;
import com.wheelGo.model.payments.PaymentResponse;
import com.wheelGo.model.promotions.Promotion;
import com.wheelGo.repository.BookingRepository;
import com.wheelGo.repository.InvoiceRepository;
import com.wheelGo.repository.PaymentRepository;
import com.wheelGo.repository.PromotionRepository;
import com.wheelGo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private static final String DEFAULT_CURRENCY = "EUR";

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final PaymentMapper paymentMapper;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceEmailJobService invoiceEmailJobService;
    private final UserRepository userRepository;
    private final InvoicePdfService invoicePdfService;
    private final FileStorageService fileStorageService;
    private final PromotionRepository promotionRepository;
    private final CacheInvalidationService cacheInvalidationService;
    private final BookingService bookingService;

    @Transactional
    public PaymentResponse payForBooking(UUID userId, PaymentRequest request) {
        if (request == null || request.getBookingId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking id is required");
        }

        Booking booking = getUserBooking(userId, request.getBookingId());
        validatePayableBooking(booking);
        applyPromotionCodeIfPresent(booking, request.getPromotionCode());
        BigDecimal amount = resolveAmount(request, booking);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "There is no outstanding amount for this booking");
        }

        PaymentMethod method = resolveMethod(request.getMethod());
        if (hasSpecialRequest(booking) && method == PaymentMethod.CARD) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Bookings with a special request can only be paid in cash after admin review."
            );
        }

        Payment saved = method == PaymentMethod.CASH
                ? createOrRefreshCashPayment(booking, request, amount)
                : createCardPaymentAndAutoConfirm(booking, request, amount);
        Invoice invoice = ensureInvoice(saved, booking);
        evictBookingAndPaymentCaches(booking);
        return toResponse(saved, invoice);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsForUser(UUID userId) {
        List<UUID> bookingIds = bookingRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(Booking::getId)
                .toList();

        if (bookingIds.isEmpty()) {
            return List.of();
        }

        return paymentRepository.findAllByBookingIdInOrderByCreatedAtDesc(bookingIds).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsForUser(UUID userId, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getPaymentsForUser(userId);
        }

        return paymentRepository.searchPaymentsForUser(userId, keyword.trim()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentResponse getLatestPaymentForBooking(UUID userId, UUID bookingId) {
        getUserBooking(userId, bookingId);
        Payment payment = paymentRepository.findTopByBookingIdOrderByCreatedAtDesc(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));

        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsForAdmin() {
        return paymentRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsForAdmin(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getPaymentsForAdmin();
        }

        return paymentRepository.searchPaymentsForAdmin(keyword.trim()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PaymentResponse confirmCashPayment(UUID id) {
        Payment payment = getPayment(id);
        if (payment.getMethod() != PaymentMethod.CASH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only cash payments can be confirmed");
        }
        PaymentAdminUpdateRequest request = new PaymentAdminUpdateRequest();
        request.setStatus(PaymentStatus.PAID);
        return updatePaymentStatus(id, request);
    }

    @Transactional
    public PaymentResponse refundPayment(UUID id) {
        Payment payment = getPayment(id);

        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only paid payments can be refunded");
        }

        Booking booking = bookingRepository.findById(payment.getBookingId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        if (booking.getStatus() != BookingStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only cancelled bookings can be refunded");
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setUpdatedAt(LocalDateTime.now());

        Payment saved = paymentRepository.save(payment);
        Invoice invoice = invoiceRepository.findByBookingId(saved.getBookingId()).orElse(null);
        evictBookingAndPaymentCaches(booking);
        return toResponse(saved, invoice);
    }

    @Transactional
    public PaymentResponse updatePaymentStatus(UUID id, PaymentAdminUpdateRequest request) {
        Payment payment = getPayment(id);
        Booking booking = bookingRepository.findById(payment.getBookingId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        PaymentStatus targetStatus = request.getStatus();
        if (targetStatus == PaymentStatus.REFUNDED
                && booking.getStatus() != BookingStatus.CANCELLED
                && booking.getStatus() != BookingStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only cancelled or completed bookings can be refunded");
        }

        LocalDateTime now = LocalDateTime.now();
        payment.setStatus(targetStatus);
        payment.setUpdatedAt(now);
        if (targetStatus == PaymentStatus.PAID) {
            payment.setPaidAt(now);
        } else if (targetStatus == PaymentStatus.PENDING || targetStatus == PaymentStatus.FAILED) {
            payment.setPaidAt(null);
        }

        Payment saved = paymentRepository.save(payment);
        Invoice invoice = ensureInvoice(saved, booking);
        evictBookingAndPaymentCaches(booking);
        return toResponse(saved, invoice);
    }

    private Payment getPayment(UUID id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
    }

    private void validatePayableBooking(Booking booking) {
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cancelled bookings cannot be paid");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completed bookings cannot be paid");
        }
    }

    private Booking getUserBooking(UUID userId, UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        if (!booking.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot access this booking");
        }

        return booking;
    }

    private BigDecimal resolveAmount(PaymentRequest request, Booking booking) {
        if (request.getAmount() != null) {
            return request.getAmount().setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal paidAmount = paymentRepository.findAllByBookingIdOrderByCreatedAtDesc(booking.getId()).stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.PAID)
                .map(Payment::getAmount)
                .map(amount -> amount != null ? amount : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return booking.getTotalPrice()
                .subtract(paidAmount)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void applyPromotionCodeIfPresent(Booking booking, String rawCode) {
        String code = normalizePromotionCode(rawCode);
        if (code == null) {
            return;
        }

        Promotion promotion = promotionRepository.findFirstByCodeIgnoreCase(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Promotion code is invalid"));

        if (booking.getPromotionId() != null) {
            if (!booking.getPromotionId().equals(promotion.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A different promotion is already applied to this booking");
            }
            return;
        }

        validatePromotion(promotion);

        BigDecimal subtotal = normalizeMoney(booking.getBasePrice()).add(normalizeMoney(booking.getAddonPrice()));
        BigDecimal discountAmount = calculateDiscountAmount(promotion, subtotal);
        if (discountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Promotion does not reduce this booking total");
        }

        booking.setPromotionId(promotion.getId());
        booking.setDiscountAmount(discountAmount);
        booking.setTotalPrice(subtotal.subtract(discountAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP));
        booking.setUpdatedAt(LocalDateTime.now());
        bookingRepository.save(booking);

        promotion.setUsesCount((promotion.getUsesCount() != null ? promotion.getUsesCount() : 0) + 1);
        promotion.setUpdatedAt(LocalDateTime.now());
        promotionRepository.save(promotion);
    }

    private void validatePromotion(Promotion promotion) {
        LocalDateTime now = LocalDateTime.now();
        if (!Boolean.TRUE.equals(promotion.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Promotion code is not active");
        }
        if (promotion.getValidFrom() != null && promotion.getValidFrom().isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Promotion code is not active yet");
        }
        if (promotion.getValidUntil() != null && promotion.getValidUntil().isBefore(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Promotion code has expired");
        }
        if (promotion.getMaxUses() != null && promotion.getUsesCount() != null
                && promotion.getUsesCount() >= promotion.getMaxUses()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Promotion code has reached its usage limit");
        }
    }

    private BigDecimal calculateDiscountAmount(Promotion promotion, BigDecimal subtotal) {
        BigDecimal discount = switch (promotion.getDiscountType()) {
            case FIXED -> promotion.getDiscountValue();
            case PERCENTAGE -> subtotal.multiply(promotion.getDiscountValue())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        };
        return discount.min(subtotal).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return (value != null ? value : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizePromotionCode(String rawCode) {
        if (rawCode == null) {
            return null;
        }
        String code = rawCode.trim();
        return code.isEmpty() ? null : code;
    }

    private String resolveCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return DEFAULT_CURRENCY;
        }
        return currency;
    }

    private PaymentMethod resolveMethod(PaymentMethod method) {
        return method != null ? method : PaymentMethod.CARD;
    }

    private Payment createOrRefreshCashPayment(Booking booking, PaymentRequest request, BigDecimal amount) {
        Payment existingPendingPayment = paymentRepository.findAllByBookingIdOrderByCreatedAtDesc(booking.getId()).stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.PENDING)
                .findFirst()
                .orElse(null);

        if (existingPendingPayment != null) {
            existingPendingPayment.setAmount(amount);
            existingPendingPayment.setCurrency(resolveCurrency(request.getCurrency()));
            existingPendingPayment.setMethod(PaymentMethod.CASH);
            existingPendingPayment.setGatewayRef(request.getGatewayRef());
            existingPendingPayment.setUpdatedAt(LocalDateTime.now());
            return paymentRepository.save(existingPendingPayment);
        }

        Payment payment = new Payment();
        payment.setBookingId(booking.getId());
        payment.setAmount(amount);
        payment.setCurrency(resolveCurrency(request.getCurrency()));
        payment.setMethod(PaymentMethod.CASH);
        payment.setGatewayRef(request.getGatewayRef());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaidAt(null);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    private Payment createCardPaymentAndAutoConfirm(Booking booking, PaymentRequest request, BigDecimal amount) {
        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending or confirmed bookings can be paid by card");
        }
        if (bookingRepository.existsByVehicleIdAndStatusInAndStartDateLessThanAndEndDateGreaterThanAndIdNot(
                booking.getVehicleId(),
                java.util.EnumSet.of(BookingStatus.CONFIRMED, BookingStatus.ACTIVE),
                booking.getEndDate(),
                booking.getStartDate(),
                booking.getId()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This vehicle already has a confirmed booking for the selected dates."
            );
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setUpdatedAt(LocalDateTime.now());
        bookingRepository.save(booking);

        Payment payment = new Payment();
        payment.setBookingId(booking.getId());
        payment.setAmount(amount);
        payment.setCurrency(resolveCurrency(request.getCurrency()));
        payment.setMethod(PaymentMethod.CARD);
        payment.setGatewayRef(request.getGatewayRef());
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());

        Payment saved = paymentRepository.save(payment);
        expireOtherPendingPayments(booking.getId());
        bookingService.cancelPendingOverlappingBookings(booking.getId());
        return saved;
    }

    private void expireOtherPendingPayments(UUID bookingId) {
        List<Payment> pendingPayments = paymentRepository.findAllByBookingIdOrderByCreatedAtDesc(bookingId).stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.PENDING)
                .toList();
        if (pendingPayments.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        pendingPayments.forEach(payment -> {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setUpdatedAt(now);
        });
        paymentRepository.saveAll(pendingPayments);
    }

    private boolean hasSpecialRequest(Booking booking) {
        return booking.getSpecialRequest() != null && !booking.getSpecialRequest().isBlank();
    }

    private void evictBookingAndPaymentCaches(Booking booking) {
        cacheInvalidationService.evictBookings(booking.getUserId());
        cacheInvalidationService.evictBookingsForAdmin();
        cacheInvalidationService.evictVehicle(booking.getVehicleId());
    }

    private Invoice ensureInvoice(Payment payment, Booking booking) {
        if (payment.getStatus() != PaymentStatus.PAID) {
            return invoiceRepository.findByBookingId(payment.getBookingId()).orElse(null);
        }

        Invoice existing = invoiceRepository.findByBookingId(payment.getBookingId()).orElse(null);
        if (existing != null) {
            String customerEmail = findCustomerEmail(booking);
            InvoiceEmailRequest invoiceRequest = buildInvoiceRequest(existing, payment, booking, customerEmail);
            if (existing.getPdfUrl() == null || existing.getPdfUrl().isBlank()) {
                existing.setPdfUrl(fileStorageService.storeInvoicePdf(
                        existing.getInvoiceNumber(),
                        invoicePdfService.generateInvoicePdf(invoiceRequest)
                ));
                existing = invoiceRepository.save(existing);
            }
            scheduleInvoiceEmail(existing, invoiceRequest);
            return existing;
        }

        Invoice invoice = new Invoice();
        invoice.setBookingId(payment.getBookingId());
        invoice.setInvoiceNumber("INV-" + System.currentTimeMillis());
        invoice.setIssuedAt(LocalDateTime.now());
        invoice.setCreatedAt(LocalDateTime.now());
        String customerEmail = findCustomerEmail(booking);
        InvoiceEmailRequest invoiceRequest = buildInvoiceRequest(invoice, payment, booking, customerEmail);
        invoice.setPdfUrl(fileStorageService.storeInvoicePdf(
                invoice.getInvoiceNumber(),
                invoicePdfService.generateInvoicePdf(invoiceRequest)
        ));
        Invoice saved = invoiceRepository.save(invoice);

        scheduleInvoiceEmail(saved, invoiceRequest);
        return saved;
    }

    private void scheduleInvoiceEmail(Invoice invoice, InvoiceEmailRequest emailRequest) {
        if (emailRequest.recipientEmail() == null || emailRequest.recipientEmail().isBlank()) {
            return;
        }

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            invoiceEmailJobService.sendInvoiceEmail(emailRequest);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                invoiceEmailJobService.sendInvoiceEmail(emailRequest);
            }
        });
    }

    private String findCustomerEmail(Booking booking) {
        return userRepository.findById(booking.getUserId())
                .map(user -> user.getEmail())
                .orElse(null);
    }

    private InvoiceEmailRequest buildInvoiceRequest(Invoice invoice, Payment payment, Booking booking, String customerEmail) {
        return new InvoiceEmailRequest(
                customerEmail,
                booking.getId(),
                invoice.getInvoiceNumber(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaidAt(),
                booking.getStartDate(),
                booking.getEndDate()
        );
    }

    private PaymentResponse toResponse(Payment payment) {
        return toResponse(payment, invoiceRepository.findByBookingId(payment.getBookingId()).orElse(null));
    }

    private PaymentResponse toResponse(Payment payment, Invoice invoice) {
        PaymentResponse response = paymentMapper.toResponse(payment);
        response.setInvoiceNumber(invoice != null ? invoice.getInvoiceNumber() : null);
        return response;
    }
}
