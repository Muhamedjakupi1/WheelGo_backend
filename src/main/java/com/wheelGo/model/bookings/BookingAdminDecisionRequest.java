package com.wheelGo.model.bookings;

import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class BookingAdminDecisionRequest {
    @DecimalMin(value = "0.00", message = "Addon charge cannot be negative")
    private BigDecimal addonCharge = BigDecimal.ZERO;

    private String addonName;
    private String note;
}
