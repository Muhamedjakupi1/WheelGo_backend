package com.wheelGo.service;

import com.wheelGo.mapper.InvoiceMapper; // Shto këtë import
import com.wheelGo.model.invoices.CreateInvoiceRequest;
import com.wheelGo.model.invoices.Invoice;
import com.wheelGo.model.invoices.InvoiceResponse; // Shto këtë import
import com.wheelGo.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@Setter
@RequiredArgsConstructor
public class InvoiceService {
    private final InvoiceRepository invoicesRepository;
    private final InvoiceMapper invoiceMapper;

    @Transactional
    public InvoiceResponse createInvoice(CreateInvoiceRequest request) {
        invoicesRepository.findByBookingId(request.getBookingID())
                .ifPresent(i -> {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The invoice for this booking already exists!");
                });

        Invoice invoices = new Invoice();
        invoices.setBookingId(request.getBookingID());
        invoices.setCreatedAt(request.getDueAt());
        invoices.setInvoiceNumber("INV-" + System.currentTimeMillis());

        Invoice saved = invoicesRepository.save(invoices);

        sendEmailAsync(saved.getInvoiceNumber());

        return invoiceMapper.toResponse(saved);
    }

    public InvoiceResponse getInvoiceByBooking(UUID bookingId) {
        Invoice invoice = invoicesRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "This invoice does not exist"));

        return invoiceMapper.toResponse(invoice);
    }

    @Async
    public void sendEmailAsync(String invoiceNumber) {
        System.out.println("Email is sent for " + invoiceNumber);
    }
}