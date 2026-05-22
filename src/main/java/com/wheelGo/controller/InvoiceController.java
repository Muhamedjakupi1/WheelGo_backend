package com.wheelGo.controller;

import com.wheelGo.model.invoices.InvoiceRequest;
import com.wheelGo.model.invoices.InvoiceResponse;
import com.wheelGo.service.InvoiceService;
import com.wheelGo.tools.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoices", description = "Endpoints for managing invoices")
public class InvoiceController {
    private final InvoiceService invoicesService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create Invoice")
    public ResponseEntity<InvoiceResponse> create(@RequestBody @Valid InvoiceRequest request) {
        return ResponseEntity.ok(invoicesService.createInvoice(request));
    }

    @GetMapping("/booking/{bookingId}/pdf")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Download invoice PDF for a booking")
    public ResponseEntity<byte[]> downloadByBooking(@PathVariable UUID bookingId) {
        InvoiceService.InvoiceDownload download = invoicesService.downloadInvoicePdf(
                bookingId,
                SecurityUtils.getCurrentPrincipal()
        );

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(download.filename())
                        .build()
                .toString())
                .body(download.content());
    }

    @PostMapping("/booking/{bookingId}/email")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Send invoice email for a booking")
    public ResponseEntity<InvoiceResponse> sendEmailByBooking(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(invoicesService.sendInvoiceEmailForBooking(
                bookingId,
                SecurityUtils.getCurrentPrincipal()
        ));
    }
}
