package com.wheelGo.repository;

import com.wheelGo.model.invoices.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    Optional<Invoice> findByBookingId(UUID bookingId);

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
}
