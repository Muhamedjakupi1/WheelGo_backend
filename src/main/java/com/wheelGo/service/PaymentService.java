package com.wheelGo.service;

import com.wheelGo.mapper.PaymentMapper;
import com.wheelGo.model.bookings.Booking;
import com.wheelGo.model.enums.BookingStatus;
import com.wheelGo.model.enums.PaymentMethod;
import com.wheelGo.model.enums.PaymentStatus;
import com.wheelGo.model.invoices.Invoice;
import com.wheelGo.model.payments.Payment;
import com.wheelGo.model.payments.PaymentRequest;
import com.wheelGo.model.payments.PaymentResponse;
import com.wheelGo.repository.BookingRepository;
import com.wheelGo.repository.InvoiceRepository;
import com.wheelGo.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @Transactional
    public PaymentResponse payForBooking(UUID userId, PaymentRequest request) {
        if (request == null || request.getBookingId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking id is required");
        }

        Booking booking = getUserBooking(userId, request.getBookingId());
        BigDecimal amount = resolveAmount(request, booking);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "There is no outstanding amount for this booking");
        }

        Payment payment = new Payment();
        payment.setBookingId(booking.getId());
        payment.setAmount(amount);
        payment.setCurrency(resolveCurrency(request.getCurrency()));
        payment.setMethod(resolveMethod(request.getMethod()));
        payment.setGatewayRef(request.getGatewayRef());
        payment.setStatus(resolveInitialStatus(request, payment.getMethod()));
        payment.setPaidAt(resolvePaidAt(request, payment.getStatus()));
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());

        Payment saved = paymentRepository.save(payment);
        Invoice invoice = ensureInvoice(saved);

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
        String normalizedKeyword = normalizeKeyword(keyword);
        if (normalizedKeyword == null) {
            return getPaymentsForUser(userId);
        }

        return paymentRepository.searchPaymentsForUser(userId, normalizedKeyword).stream()
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
        String normalizedKeyword = normalizeKeyword(keyword);
        if (normalizedKeyword == null) {
            return getPaymentsForAdmin();
        }

        return paymentRepository.searchPaymentsForAdmin(normalizedKeyword).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PaymentResponse confirmCashPayment(UUID id) {
        Payment payment = getPayment(id);

        if (payment.getMethod() != PaymentMethod.CASH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only cash payments can be confirmed");
        }

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refunded payments cannot be confirmed");
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());

        Payment saved = paymentRepository.save(payment);
        Invoice invoice = ensureInvoice(saved);

        return toResponse(saved, invoice);
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

        return toResponse(paymentRepository.save(payment));
    }

    private Payment getPayment(UUID id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }

        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
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
            return request.getAmount();
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

    private String resolveCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return DEFAULT_CURRENCY;
        }
        return currency;
    }

    private PaymentMethod resolveMethod(PaymentMethod method) {
        return method != null ? method : PaymentMethod.CARD;
    }

    private PaymentStatus resolveInitialStatus(PaymentRequest request, PaymentMethod method) {
        if (request.getStatus() != null) {
            return request.getStatus();
        }
        return method == PaymentMethod.CASH ? PaymentStatus.PENDING : PaymentStatus.PAID;
    }

    private LocalDateTime resolvePaidAt(PaymentRequest request, PaymentStatus status) {
        if (request.getPaidAt() != null) {
            return request.getPaidAt();
        }
        return status == PaymentStatus.PAID ? LocalDateTime.now() : null;
    }

    private Invoice ensureInvoice(Payment payment) {
        if (payment.getStatus() != PaymentStatus.PAID) {
            return invoiceRepository.findByBookingId(payment.getBookingId()).orElse(null);
        }

        Invoice invoice = invoiceRepository.findByBookingId(payment.getBookingId())
                .orElseGet(() -> {
                    Invoice created = new Invoice();
                    created.setBookingId(payment.getBookingId());
                    created.setInvoiceNumber("INV-" + System.currentTimeMillis());
                    created.setIssuedAt(LocalDateTime.now());
                    created.setCreatedAt(LocalDateTime.now());
                    return invoiceRepository.save(created);
                });

        invoiceEmailJobService.sendInvoiceEmail(invoice.getInvoiceNumber());
        return invoice;
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
