package com.wheelGo.model.vehicle_images;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter @Setter
public class VehicleImageResponse {
    private UUID id;
    private String url;
    private boolean isPrimary;
}

