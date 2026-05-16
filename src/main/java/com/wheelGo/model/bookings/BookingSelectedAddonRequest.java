package com.wheelGo.model.bookings;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class BookingSelectedAddonRequest {
    @NotNull(message = "Addon id is required")
    private UUID addonId;

    @Positive(message = "Addon quantity must be greater than zero")
    private Integer quantity;
}
