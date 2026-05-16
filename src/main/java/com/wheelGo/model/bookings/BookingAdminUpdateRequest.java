package com.wheelGo.model.bookings;

import com.wheelGo.model.enums.BookingStatus;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class BookingAdminUpdateRequest {
    private LocalDate startDate;
    private LocalDate endDate;
    private BookingStatus status;

    @DecimalMin(value = "0.00", message = "Addon charge cannot be negative")
    private BigDecimal addonCharge;

    private String addonName;
    private String note;
}
