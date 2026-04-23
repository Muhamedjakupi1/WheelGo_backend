package com.wheelGo.model.vehiclecategories;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter @Setter
public class VehicleCategoryResponse {
    private UUID id;
    private String name;
    private String description;
    private String imageUrl;
}
