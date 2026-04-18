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

    public static VehicleResponse from(Vehicle v) {
        VehicleResponse res = new VehicleResponse();
        res.setId(v.getId());
        res.setCategoryId(v.getCategory().getId());
        res.setCategoryName(v.getCategory().getName());

        if (v.getLocation() != null) {
            res.setLocationId(v.getLocation().getId());
            res.setLocationName(v.getLocation().getName());
        }

        res.setPlateNumber(v.getPlateNumber());
        res.setMake(v.getMake());
        res.setModel(v.getModel());
        res.setYear(v.getYear());
        res.setColor(v.getColor());
        res.setFuelType(v.getFuelType());
        res.setTransmission(v.getTransmission());
        res.setSeats(v.getSeats());
        res.setDailyRate(v.getDailyRate());
        res.setStatus(v.getStatus());
        res.setMileage(v.getMileage());
        return res;
    }
}
