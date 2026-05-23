package com.wheelGo.controller;

import com.wheelGo.model.driver_licenses.DriverLicenseResponse;
import com.wheelGo.model.driver_licenses.DriverLicenseUpdateRequest;
import com.wheelGo.model.driver_licenses.DriverLicenseVerificationResponse;
import com.wheelGo.service.DriverLicenseService;
import com.wheelGo.tools.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/driver-license")
@RequiredArgsConstructor
public class DriverLicenseController {

    private final DriverLicenseService driverLicenseService;

    @GetMapping("/me")
    public ResponseEntity<DriverLicenseResponse> getMyLicense() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(driverLicenseService.getMyLicense(userId));
    }

    @PutMapping("/me")
    public ResponseEntity<DriverLicenseResponse> updateMyLicense(@RequestBody DriverLicenseUpdateRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(driverLicenseService.upsertMyLicense(userId, request));
    }

    @PostMapping(value = "/me/front-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DriverLicenseResponse> uploadFrontImage(@RequestParam MultipartFile file) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(driverLicenseService.uploadFront(userId, file));
    }

    @PostMapping(value = "/me/back-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DriverLicenseResponse> uploadBackImage(@RequestParam MultipartFile file) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(driverLicenseService.uploadBack(userId, file));
    }

    @PostMapping(value = "/me/verify", consumes = MediaType.APPLICATION_JSON_VALUE)
    public CompletableFuture<ResponseEntity<DriverLicenseVerificationResponse>> verifyMyLicense(@RequestBody(required = false) DriverLicenseUpdateRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return driverLicenseService.verifyMyLicense(userId, request)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping(value = "/me/verify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CompletableFuture<ResponseEntity<DriverLicenseVerificationResponse>> verifyMyLicenseWithImages(
            @RequestParam String licenseNumber,
            @RequestParam String issuingCountry,
            @RequestParam String expiryDate,
            @RequestParam(required = false) MultipartFile frontImage,
            @RequestParam(required = false) MultipartFile backImage
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        DriverLicenseUpdateRequest request = new DriverLicenseUpdateRequest();
        request.setLicenseNumber(licenseNumber);
        request.setIssuingCountry(issuingCountry);
        request.setExpiryDate(java.time.LocalDate.parse(expiryDate));
        return driverLicenseService.verifyMyLicense(userId, request, frontImage, backImage)
                .thenApply(ResponseEntity::ok);
    }
}
