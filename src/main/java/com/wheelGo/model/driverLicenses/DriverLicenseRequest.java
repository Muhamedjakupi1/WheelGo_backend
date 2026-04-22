package com.wheelGo.model.driverLicenses;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter @Setter
public class DriverLicenseRequest {

    @NotBlank(message = "License number cannot be empty")
    private String licenseNumber;

    @NotBlank(message = "Issuing country cannot be empty")
    private String issuingCountry;

    @NotNull(message = "Expiry date is required")
    @Future(message = "Expiry date must be in the future")
    private LocalDate expiryDate;

    private String frontImageUrl;
    private String backImageUrl;
}