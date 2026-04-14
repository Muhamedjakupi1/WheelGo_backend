package com.wheelGo.model.vehicleCategories;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateVehicleCategories {

    @NotBlank(message = "Name is required")
    @Size(max = 60)
    private String name;

    private String description;

    private String imageUrl;
}
