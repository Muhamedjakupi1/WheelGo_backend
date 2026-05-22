package com.wheelGo.model.invoices;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record InvoiceEmailRequest(
        String recipientEmail,
        UUID bookingId,
        String invoiceNumber,
        BigDecimal amount,
        String currency,
        LocalDateTime paidAt,
        LocalDateTime bookingStart,
        LocalDateTime bookingEnd
) {
}
