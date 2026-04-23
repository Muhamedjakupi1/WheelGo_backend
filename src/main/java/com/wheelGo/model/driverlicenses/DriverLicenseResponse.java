package com.wheelGo.model.driverlicenses;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter
public class DriverLicenseResponse {

    private UUID          id;
    private UUID          userId;
    private String        licenseNumber;
    private String        issuingCountry;
    private LocalDate     expiryDate;
    private String        frontImageUrl;
    private String        backImageUrl;
    private LocalDateTime verifiedAt;
    private boolean       isVerified;
}