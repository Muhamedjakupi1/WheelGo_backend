package com.wheelGo.model.vehicleCategories;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateVehicleCategory {
    @Size(max = 60, message = "Name can not be longer than 60 characters")
    private String name;

    private String description;

    private String imageUrl;
}
