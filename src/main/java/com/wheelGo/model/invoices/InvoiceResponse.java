package com.wheelGo.model.invoices;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter
public class InvoiceResponse {
    private UUID id;
    private String invoiceNumber;
    private String pdfUrl;
    private LocalDateTime issuedAt;
    private LocalDateTime dueAt;
}
