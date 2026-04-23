package com.wheelGo.controller;


import com.wheelGo.model.invoices.InvoiceRequest;
import com.wheelGo.model.invoices.InvoiceResponse;
import com.wheelGo.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoices", description = "Endpoints for managing invoices")
public class InvoiceController {
    private final InvoiceService invoicesService;

    @PostMapping
    @Operation(summary = "Create Invoice")
    public ResponseEntity<InvoiceResponse> create(@RequestBody @Valid InvoiceRequest request) {
        return ResponseEntity.ok(invoicesService.createInvoice(request));
    }
}
