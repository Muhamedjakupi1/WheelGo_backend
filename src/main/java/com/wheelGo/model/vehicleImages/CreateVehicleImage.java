package com.wheelGo.model.vehicleImages;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter @Setter
public class CreateVehicleImage {
    @NotNull(message = "ID of car is required")
    private UUID vehicleId;

    @NotBlank(message = "URL of image is required")
    private String url;

    private boolean isPrimary = false;
}
