package com.wheelGo.model.booking_addons;

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

    public static BookingAddonResponse from(BookingAddon bookingAddon) {
        BookingAddonResponse res = new BookingAddonResponse();
        res.setId(bookingAddon.getId());
        res.setBookingId(bookingAddon.getBookingId());
        res.setAddonId(bookingAddon.getAddonId());
        res.setQuantity(bookingAddon.getQuantity());
        res.setPriceSnapshot(bookingAddon.getPriceSnapshot());
        res.setCreatedAt(bookingAddon.getCreatedAt());
        return res;
    }
}