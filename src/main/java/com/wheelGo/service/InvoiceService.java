package com.wheelGo.service;

import com.wheelGo.mapper.InvoiceMapper;
import com.wheelGo.model.bookings.Booking;
import com.wheelGo.model.enums.PaymentStatus;
import com.wheelGo.model.invoices.Invoice;
import com.wheelGo.model.invoices.InvoiceEmailRequest;
import com.wheelGo.model.invoices.InvoiceRequest;
import com.wheelGo.model.invoices.InvoiceResponse;
import com.wheelGo.model.payments.Payment;
import com.wheelGo.repository.BookingRepository;
import com.wheelGo.repository.InvoiceRepository;
import com.wheelGo.repository.PaymentRepository;
import com.wheelGo.repository.UserRepository;
import com.wheelGo.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Setter
@RequiredArgsConstructor
public class InvoiceService {
    private final InvoiceRepository invoicesRepository;
    private final InvoiceMapper invoiceMapper;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final InvoicePdfService invoicePdfService;
    private final FileStorageService fileStorageService;
    private final InvoiceEmailJobService invoiceEmailJobService;

    @Transactional
    public InvoiceResponse createInvoice(InvoiceRequest request) {
        invoicesRepository.findByBookingId(request.getBookingID())
                .ifPresent(i -> {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The invoice for this booking already exists!");
                });

        Invoice invoice = new Invoice();
        invoice.setBookingId(request.getBookingID());
        invoice.setDueAt(request.getDueAt());
        invoice.setInvoiceNumber("INV-" + System.currentTimeMillis());

        return invoiceMapper.toResponse(invoicesRepository.save(invoice));
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceByBooking(UUID bookingId) {
        Invoice invoice = invoicesRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "This invoice does not exist"));

        return invoiceMapper.toResponse(invoice);
    }

    @Transactional
    public InvoiceDownload downloadInvoicePdf(UUID bookingId, CustomUserPrincipal principal) {
        InvoiceEmailRequest pdfRequest = buildInvoiceEmailRequest(bookingId, principal).request();
        Invoice invoice = ensureInvoicePdfStored(bookingId, pdfRequest);
        return new InvoiceDownload(invoice.getInvoiceNumber() + ".pdf", invoicePdfService.generateInvoicePdf(pdfRequest));
    }

    @Transactional
    public InvoiceResponse sendInvoiceEmailForBooking(UUID bookingId, CustomUserPrincipal principal) {
        InvoiceEmailData emailData = buildInvoiceEmailRequest(bookingId, principal);
        ensureInvoicePdfStored(bookingId, emailData.request());

        if (emailData.request().recipientEmail() == null || emailData.request().recipientEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer email is missing");
        }

        invoiceEmailJobService.sendInvoiceEmailNow(emailData.request());
        return invoiceMapper.toResponse(emailData.invoice());
    }

    private InvoiceEmailData buildInvoiceEmailRequest(UUID bookingId, CustomUserPrincipal principal) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        authorizeInvoiceDownload(booking, principal);

        Payment payment = paymentRepository.findAllByBookingIdOrderByCreatedAtDesc(bookingId).stream()
                .filter(candidate -> candidate.getStatus() == PaymentStatus.PAID)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invoice is available only after payment"));

        Invoice invoice = invoicesRepository.findByBookingId(bookingId)
                .orElseGet(() -> createInvoiceForPaidBooking(booking));

        String customerEmail = userRepository.findById(booking.getUserId())
                .map(user -> user.getEmail())
                .orElse(principal != null ? principal.getEmail() : null);

        InvoiceEmailRequest request = new InvoiceEmailRequest(
                customerEmail,
                booking.getId(),
                invoice.getInvoiceNumber(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaidAt(),
                booking.getStartDate(),
                booking.getEndDate()
        );

        return new InvoiceEmailData(invoice, request);
    }

    private Invoice ensureInvoicePdfStored(UUID bookingId, InvoiceEmailRequest pdfRequest) {
        Invoice invoice = invoicesRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "This invoice does not exist"));

        if (invoice.getPdfUrl() == null || invoice.getPdfUrl().isBlank()) {
            invoice.setPdfUrl(fileStorageService.storeInvoicePdf(
                    invoice.getInvoiceNumber(),
                    invoicePdfService.generateInvoicePdf(pdfRequest)
            ));
            invoice = invoicesRepository.save(invoice);
        }

        return invoice;
    }

    private Invoice createInvoiceForPaidBooking(Booking booking) {
        Invoice invoice = new Invoice();
        invoice.setBookingId(booking.getId());
        invoice.setInvoiceNumber("INV-" + System.currentTimeMillis());
        invoice.setIssuedAt(LocalDateTime.now());
        invoice.setCreatedAt(LocalDateTime.now());
        return invoicesRepository.save(invoice);
    }

    private void authorizeInvoiceDownload(Booking booking, CustomUserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        if ("ADMIN".equals(principal.getRole()) || "SUPER_ADMIN".equals(principal.getRole())) {
            return;
        }

        if (!booking.getUserId().equals(principal.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot access this invoice");
        }
    }

    public record InvoiceDownload(String filename, byte[] content) {
    }

    private record InvoiceEmailData(Invoice invoice, InvoiceEmailRequest request) {
    }
}
