package com.wheelGo.model.driverLicenses;

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

    public static DriverLicenseResponse from(DriverLicense d) {
        DriverLicenseResponse r = new DriverLicenseResponse();
        r.setId(d.getId());
        r.setUserId(d.getUser().getId());
        r.setLicenseNumber(d.getLicenseNumber());
        r.setIssuingCountry(d.getIssuingCountry());
        r.setExpiryDate(d.getExpiryDate());
        r.setFrontImageUrl(d.getFrontImageUrl());
        r.setBackImageUrl(d.getBackImageUrl());
        r.setVerifiedAt(d.getVerifiedAt());
        r.setVerified(d.getVerifiedAt() != null);
        return r;
    }
}