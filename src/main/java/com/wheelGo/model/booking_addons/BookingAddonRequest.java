package com.wheelGo.model.booking_addons;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class BookingAddonRequest {
    private UUID bookingId;
    private UUID addonId;
    private Short quantity;
    private BigDecimal priceSnapshot;
}