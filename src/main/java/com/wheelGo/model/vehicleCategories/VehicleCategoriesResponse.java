package com.wheelGo.model.vehicleCategories;

import com.wheelGo.model.locations.LocationResponse;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter
public class VehicleCategoriesResponse {
    private UUID id;
    private String name;
    private String description;
    private String imageUrl;

    public static VehicleCategoriesResponse from (VehicleCategories v) {
        VehicleCategoriesResponse vs = new VehicleCategoriesResponse();
        vs.setId(v.getId());
        vs.setName(v.getName());
        vs.setDescription(v.getDescription());
        vs.setImageUrl(v.getImageUrl());
        return vs;
    }
}
