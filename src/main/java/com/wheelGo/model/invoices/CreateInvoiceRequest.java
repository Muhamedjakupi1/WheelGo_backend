package com.wheelGo.model.invoices;


import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter
public class CreateInvoiceRequest {
    @NotNull(message = "Booking ID is required")
    private UUID bookingID;

    private LocalDateTime dueAt;
}
