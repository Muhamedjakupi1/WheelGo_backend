package com.wheelGo.model.bookingaddons;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class BookingAddonResponse {
    private UUID id;
    private UUID bookingId;
    private UUID addonId;
    private Short quantity;
    private BigDecimal priceSnapshot;
    private LocalDateTime createdAt;
}