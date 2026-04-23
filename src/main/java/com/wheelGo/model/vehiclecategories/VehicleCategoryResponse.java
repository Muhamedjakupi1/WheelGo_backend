package com.wheelGo.model.vehicleCategories;

import com.wheelGo.model.locations.LocationResponse;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter
public class VehicleCategoryResponse {
    private UUID id;
    private String name;
    private String description;
    private String imageUrl;
}
