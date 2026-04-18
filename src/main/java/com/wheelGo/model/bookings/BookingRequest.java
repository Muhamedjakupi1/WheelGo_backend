package com.wheelGo.model.bookings;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class BookingRequest {
    private UUID userId;
    private UUID vehicleId;
    private UUID pickupLocationId;
    private UUID dropoffLocationId;
    private UUID promotionId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer totalDays;
    private BigDecimal basePrice;
    private BigDecimal discountAmount;
    private BigDecimal addonPrice;
    private BigDecimal totalPrice;
    private String status;
    private String notes;
}