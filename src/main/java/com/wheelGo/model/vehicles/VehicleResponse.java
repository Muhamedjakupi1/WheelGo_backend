package com.wheelGo.model.vehicles;

import com.wheelGo.model.enums.FuelType;
import com.wheelGo.model.enums.Transmission;
import com.wheelGo.model.enums.VehicleStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter @Setter
public class VehicleResponse {
    private UUID id;
    private UUID categoryId;
    private String categoryName;
    private UUID locationId;
    private String locationName;
    private String plateNumber;
    private String make;
    private String model;
    private Short year;
    private String color;
    private FuelType fuelType;
    private Transmission transmission;
    private Short seats;
    private BigDecimal dailyRate;
    private VehicleStatus status;
    private Integer mileage;
}
