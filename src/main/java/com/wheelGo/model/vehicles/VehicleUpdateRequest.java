package com.wheelGo.model.vehicles;

import com.wheelGo.model.enums.FuelType;
import com.wheelGo.model.enums.Transmission;
import com.wheelGo.model.enums.VehicleStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class VehicleUpdateRequest {

    private UUID categoryId;
    private UUID locationId;
    private Boolean clearLocation;
    private String plateNumber;
    private String make;
    private String model;

    @Min(1900)
    private Short year;

    private String color;
    private String vin;
    private FuelType fuelType;
    private Transmission transmission;
    private Short seats;

    @Positive(message = "Daily rate must be positive")
    private BigDecimal dailyRate;

    private VehicleStatus status;

    @PositiveOrZero(message = "Mileage cannot be negative")
    private Integer mileage;
}
