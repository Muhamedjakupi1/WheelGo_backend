package com.wheelGo.model.reviews;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter @Setter
public class ReviewRequest {

    @NotNull ( message = "Booking ID is required")
    private UUID bookingId;

    @NotNull
    private UUID vehicleId;

    @NotNull
    @Min(1) @Max(5)
    private int rating;

    private String comment;
}
