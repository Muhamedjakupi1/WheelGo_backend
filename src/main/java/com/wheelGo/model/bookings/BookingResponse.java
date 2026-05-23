package com.wheelGo.model.bookings;

import com.wheelGo.model.enums.BookingStatus;
import com.wheelGo.model.enums.PaymentMethod;
import com.wheelGo.model.enums.PaymentStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
    private BigDecimal paidAmount;
    private BigDecimal outstandingAmount;
    private BookingStatus status;
    private String notes;
    private String specialRequest;
    private Boolean babySeatRequested;
    private List<String> addonNames;
    private String vehicleName;
    private String vehicleImageUrl;
    private String locationName;
    private String customerEmail;
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;
    private String invoiceNumber;
    private Boolean reviewEligible;
    private LocalDateTime reviewEligibleAt;
    private LocalDateTime reviewSubmittedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
