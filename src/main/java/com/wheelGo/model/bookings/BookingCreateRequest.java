package com.wheelGo.model.bookings;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class BookingCreateRequest {
    @NotNull(message = "Vehicle id is required")
    private UUID vehicleId;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    private Boolean babySeatRequested = false;

    @PositiveOrZero(message = "Baby seat quantity cannot be negative")
    private Integer babySeatQuantity = 0;

    @PositiveOrZero(message = "Bluetooth quantity cannot be negative")
    private Integer bluetoothQuantity = 0;

    private String specialRequest;
}
