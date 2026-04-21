package com.wheelGo.model.invoices;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "invoices")
@Getter @Setter @NoArgsConstructor
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

   @Column (name = "booking_id", nullable = false, unique = true)
    private UUID bookingId;

   @Column (name = "invoice_number", nullable = false, unique = true, length = 40)
    private String invoiceNumber;

   @Column (name = "pdf_url")
    private String pdfUrl;

   @Column (name = "issued_at", nullable = false)
    private LocalDateTime issuedAt = LocalDateTime.now();

   @Column (name = "due_at")
    private LocalDateTime dueAt;

   @Column (name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

}
