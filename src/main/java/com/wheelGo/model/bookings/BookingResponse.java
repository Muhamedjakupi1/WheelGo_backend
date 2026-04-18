package com.wheelGo.model.bookings;

import com.wheelGo.model.enums.BookingStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class BookingResponse {
    private UUID id;
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
    private BookingStatus status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BookingResponse from(Booking booking) {
        BookingResponse res = new BookingResponse();
        res.setId(booking.getId());
        res.setUserId(booking.getUserId());
        res.setVehicleId(booking.getVehicleId());
        res.setPickupLocationId(booking.getPickupLocationId());
        res.setDropoffLocationId(booking.getDropoffLocationId());
        res.setPromotionId(booking.getPromotionId());
        res.setStartDate(booking.getStartDate());
        res.setEndDate(booking.getEndDate());
        res.setTotalDays(booking.getTotalDays());
        res.setBasePrice(booking.getBasePrice());
        res.setDiscountAmount(booking.getDiscountAmount());
        res.setAddonPrice(booking.getAddonPrice());
        res.setTotalPrice(booking.getTotalPrice());
        res.setStatus(booking.getStatus());
        res.setNotes(booking.getNotes());
        res.setCreatedAt(booking.getCreatedAt());
        res.setUpdatedAt(booking.getUpdatedAt());
        return res;
    }
}