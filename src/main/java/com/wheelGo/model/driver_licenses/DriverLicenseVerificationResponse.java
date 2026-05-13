package com.wheelGo.model.driver_licenses;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DriverLicenseVerificationResponse {
    private DriverLicenseResponse license;
    private boolean documentVisible;
    private boolean driverLicenseLike;
    private boolean imageQualityOk;
    private String extractedLicenseNumber;
    private String extractedName;
    private String extractedExpiryDate;
    private String extractedCountry;
    private List<String> tamperingSignals;
    private double confidence;
    private String recommendation;
    private boolean requiredFieldsExtracted;
    private boolean profileNameMatches;
    private boolean verified;
    private String verdict;
}
