package com.wheelGo.model.vehicles;

import com.wheelGo.model.enums.FuelType;
import com.wheelGo.model.enums.Transmission;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter @Setter
public class CreateVehicle {
    @NotNull(message = "ID of category is required")
    private UUID categoryId;

    private UUID locationId;

    @NotBlank(message = "Plate number is required")
    private String plateNumber;

    @NotBlank(message = "The maker's name is required")
    private String make;

    @NotBlank(message = "Model is required")
    private String model;

    @NotNull
    @Min(1900)
    private Short year;

    private String color;
    private String vin;
    private FuelType fuelType;
    private Transmission transmission;
    private Short seats;

    @NotNull
    @Positive
    private BigDecimal dailyRate;

    private Integer mileage;
}
