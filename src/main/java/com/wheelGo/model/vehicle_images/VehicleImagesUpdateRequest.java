package com.wheelGo.model.vehicle_images;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VehicleImagesUpdateRequest {

    private String url;

    private Boolean isPrimary;
}
