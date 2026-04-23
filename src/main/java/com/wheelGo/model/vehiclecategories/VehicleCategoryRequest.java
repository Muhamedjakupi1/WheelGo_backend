package com.wheelGo.model.vehiclecategories;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class VehicleCategoryRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 60)
    private String name;

    private String description;

    private String imageUrl;
}
