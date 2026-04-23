package com.wheelGo.model.bookings;

import com.wheelGo.model.enums.BookingStatus;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter
public class BookingUpdateRequest {
    private BookingStatus status;

    private UUID dropoffLocationId;

    private LocalDateTime endDate;

    private String notes;
}