package com.wheelGo.model.driverLicenses;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter @Setter
public class DriverLicenseUpdateRequest {
    private String    licenseNumber;
    private String    issuingCountry;
    private LocalDate expiryDate;
    private String    frontImageUrl;
    private String    backImageUrl;
}