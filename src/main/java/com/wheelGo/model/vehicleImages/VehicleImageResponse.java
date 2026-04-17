package com.wheelGo.model.vehicleImages;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter @Setter
public class VehicleImageResponse {
    private UUID id;
    private String url;
    private boolean isPrimary;

    public static VehicleImageResponse from(VehicleImage img) {
        VehicleImageResponse res = new VehicleImageResponse();
        res.setId(img.getId());
        res.setUrl(img.getUrl());
        res.setPrimary(img.isPrimary());
        return res;
    }
}

