package com.wheelGo.model.reviews;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter
public class ReviewResponse {
    private UUID id;
    private UUID bookingId;
    private UUID userId;
    private UUID vehicleId;
    private String customerEmail;
    private String vehicleName;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;
}
