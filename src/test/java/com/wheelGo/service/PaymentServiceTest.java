package com.wheelGo.service;

import com.wheelGo.mapper.PaymentMapper;
import com.wheelGo.model.bookings.Booking;
import com.wheelGo.model.enums.BookingStatus;
import com.wheelGo.model.enums.PaymentMethod;
import com.wheelGo.model.enums.PaymentStatus;
import com.wheelGo.model.invoices.Invoice;
import com.wheelGo.model.payments.Payment;
import com.wheelGo.model.payments.PaymentAdminUpdateRequest;
import com.wheelGo.model.payments.PaymentRequest;
import com.wheelGo.model.payments.PaymentResponse;
import com.wheelGo.model.user.User;
import com.wheelGo.repository.BookingRepository;
import com.wheelGo.repository.InvoiceRepository;
import com.wheelGo.repository.PaymentRepository;
import com.wheelGo.repository.PromotionRepository;
import com.wheelGo.repository.UserRepository;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private PaymentMapper paymentMapper;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private InvoiceEmailJobService invoiceEmailJobService;
    @Mock private UserRepository userRepository;
    @Mock private InvoicePdfService invoicePdfService;
    @Mock private FileStorageService fileStorageService;
    @Mock private PromotionRepository promotionRepository;
    @Mock private CacheInvalidationService cacheInvalidationService;
    @Mock private BookingService bookingService;
    @InjectMocks private PaymentService paymentService;

    private UUID userId;
    private Booking booking;
    private Invoice invoice;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        booking = new Booking();
        booking.setId(UUID.randomUUID());
        booking.setUserId(userId);
        booking.setVehicleId(UUID.randomUUID());
        booking.setStartDate(LocalDateTime.now().plusDays(1));
        booking.setEndDate(LocalDateTime.now().plusDays(3));
        booking.setBasePrice(new BigDecimal("100.00"));
        booking.setAddonPrice(new BigDecimal("20.00"));
        booking.setDiscountAmount(BigDecimal.ZERO);
        booking.setTotalPrice(new BigDecimal("120.00"));
        booking.setStatus(BookingStatus.PENDING);

        invoice = new Invoice();
        invoice.setBookingId(booking.getId());
        invoice.setInvoiceNumber("INV-1");
        invoice.setPdfUrl("/invoice.pdf");

        User user = new User();
        user.setId(userId);
        user.setEmail("user@example.com");

        lenient().when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        lenient().when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        lenient().when(invoiceRepository.findByBookingId(booking.getId())).thenReturn(Optional.of(invoice));
        lenient().when(paymentMapper.toResponse(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            PaymentResponse response = new PaymentResponse();
            response.setId(payment.getId());
            response.setBookingId(payment.getBookingId());
            response.setAmount(payment.getAmount());
            response.setCurrency(payment.getCurrency());
            response.setMethod(payment.getMethod());
            response.setStatus(payment.getStatus());
            response.setPaidAt(payment.getPaidAt());
            response.setCreatedAt(payment.getCreatedAt());
            response.setUpdatedAt(payment.getUpdatedAt());
            return response;
        });
    }

    @Test
    void should_reject_card_payment_when_booking_has_special_request() {
        booking.setSpecialRequest("Airport pickup");

        PaymentRequest request = new PaymentRequest();
        request.setBookingId(booking.getId());
        request.setMethod(PaymentMethod.CARD);

        assertThatThrownBy(() -> paymentService.payForBooking(userId, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("only be paid in cash");
    }

    @Test
    void should_auto_confirm_booking_and_cancel_pending_overlaps_for_card_payment() {
        PaymentRequest request = new PaymentRequest();
        request.setBookingId(booking.getId());
        request.setMethod(PaymentMethod.CARD);
        request.setCurrency("EUR");

        Payment savedPayment = new Payment();
        savedPayment.setId(UUID.randomUUID());
        savedPayment.setBookingId(booking.getId());
        savedPayment.setAmount(new BigDecimal("120.00"));
        savedPayment.setCurrency("EUR");
        savedPayment.setMethod(PaymentMethod.CARD);
        savedPayment.setStatus(PaymentStatus.PAID);
        savedPayment.setPaidAt(LocalDateTime.now());
        savedPayment.setCreatedAt(LocalDateTime.now());
        savedPayment.setUpdatedAt(LocalDateTime.now());

        when(paymentRepository.findAllByBookingIdOrderByCreatedAtDesc(booking.getId())).thenReturn(List.of());
        when(bookingRepository.existsByVehicleIdAndStatusInAndStartDateLessThanAndEndDateGreaterThanAndIdNot(
                booking.getVehicleId(),
                java.util.EnumSet.of(BookingStatus.CONFIRMED, BookingStatus.ACTIVE),
                booking.getEndDate(),
                booking.getStartDate(),
                booking.getId()
        )).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        PaymentResponse response = paymentService.payForBooking(userId, request);

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(bookingService).cancelPendingOverlappingBookings(booking.getId());
    }

    @Test
    void should_allow_admin_to_mark_payment_failed() {
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setBookingId(booking.getId());
        payment.setAmount(new BigDecimal("120.00"));
        payment.setCurrency("EUR");
        payment.setMethod(PaymentMethod.CASH);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());

        PaymentAdminUpdateRequest request = new PaymentAdminUpdateRequest();
        request.setStatus(PaymentStatus.FAILED);

        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.updatePaymentStatus(payment.getId(), request);

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getPaidAt()).isNull();
        verify(bookingService, never()).cancelPendingOverlappingBookings(any());
    }
}
