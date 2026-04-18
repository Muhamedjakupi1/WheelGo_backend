package com.wheelGo.model.vehicles;

import com.wheelGo.model.enums.VehicleStatus;
import com.wheelGo.model.locations.Location;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class UpdateVehicles {

    private UUID locationid;  // duhet te shikohet edhe nje here

    private String color;


    @PositiveOrZero(message = "Daily rate must be zero or positive")
    private BigDecimal dailyRate;

    @NotBlank(message = "Status is required")
    private VehicleStatus status;

    @PositiveOrZero(message = "Mileage cannot be negative")
    private Integer mileage;
}
