package com.wheelGo.model.booking_addons;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class BookingAddonUpdateRequest {
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}