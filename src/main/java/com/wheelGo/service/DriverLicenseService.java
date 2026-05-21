package com.wheelGo.service;

import com.wheelGo.model.driver_licenses.DriverLicense;
import com.wheelGo.model.driver_licenses.DriverLicenseResponse;
import com.wheelGo.model.driver_licenses.DriverLicenseUpdateRequest;
import com.wheelGo.model.driver_licenses.DriverLicenseVerificationResponse;
import com.wheelGo.model.user.User;
import com.wheelGo.repository.DriverLicenseRepository;
import com.wheelGo.repository.UserRepository;
import com.wheelGo.tools.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DriverLicenseService {

    private final DriverLicenseRepository driverLicenseRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final OllamaDriverLicenseVerificationService ollamaDriverLicenseVerificationService;

    @Transactional(readOnly = true)
    public DriverLicenseResponse getMyLicense(UUID userId) {
        User user = findCurrentUser(userId);
        DriverLicense license = driverLicenseRepository.findByUser_Id(userId).orElse(null);
        return toResponse(license, userId);
    }

    @Transactional
    public DriverLicenseResponse upsertMyLicense(UUID userId, DriverLicenseUpdateRequest request) {
        User user = findCurrentUser(userId);
        DriverLicense license = driverLicenseRepository.findByUser_Id(userId)
                .orElseGet(() -> createEmptyLicense(user));

        if (request.getLicenseNumber() != null) {
            license.setLicenseNumber(normalizeRequiredText(request.getLicenseNumber(), "License number"));
        }
        if (request.getIssuingCountry() != null) {
            license.setIssuingCountry(normalizeRequiredText(request.getIssuingCountry(), "Issuing country"));
        }
        if (request.getExpiryDate() != null) {
            license.setExpiryDate(request.getExpiryDate());
        }

        license.setUpdatedAt(LocalDateTime.now());
        clearVerificationState(license);
        DriverLicense saved = driverLicenseRepository.save(license);
        return toResponse(saved, userId);
    }

    @Transactional
    public DriverLicenseResponse uploadFront(UUID userId, MultipartFile file) {
        return uploadImage(userId, file, true);
    }

    @Transactional
    public DriverLicenseResponse uploadBack(UUID userId, MultipartFile file) {
        return uploadImage(userId, file, false);
    }

    @Transactional
    public DriverLicenseVerificationResponse verifyMyLicense(UUID userId, DriverLicenseUpdateRequest request) {
        return verifyMyLicense(userId, request, null, null);
    }

    @Transactional
    public DriverLicenseVerificationResponse verifyMyLicense(UUID userId,
                                                            DriverLicenseUpdateRequest request,
                                                            MultipartFile frontImage,
                                                            MultipartFile backImage) {
        User user = findCurrentUser(userId);
        DriverLicense license = driverLicenseRepository.findByUser_Id(userId)
                .orElseGet(() -> createEmptyLicense(user));

        applyRequiredDetailsForVerification(license, request);
        if (frontImage != null && !frontImage.isEmpty()) {
            license.setFrontImageUrl(fileStorageService.storeDriverLicenseImage(frontImage, "front"));
        }
        if (backImage != null && !backImage.isEmpty()) {
            license.setBackImageUrl(fileStorageService.storeDriverLicenseImage(backImage, "back"));
        }
        if (isBlank(license.getFrontImageUrl()) || isBlank(license.getBackImageUrl())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload both front and back images before verification");
        }
        if (isBlank(license.getLicenseNumber()) || isBlank(license.getIssuingCountry()) || isPendingExpiryDate(license.getExpiryDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter and save license number, issuing country, and expiry date before verification");
        }

        license.setUpdatedAt(LocalDateTime.now());
        clearVerificationState(license);
        driverLicenseRepository.save(license);

        DriverLicenseVerificationResponse response = ollamaDriverLicenseVerificationService.verify(
                fileStorageService.resolveStoredUpload(license.getFrontImageUrl()),
                fileStorageService.resolveStoredUpload(license.getBackImageUrl())
        );

        applyVerificationResult(license, response.isVerified());
        DriverLicense saved = driverLicenseRepository.save(license);

        response.setLicense(toResponse(saved, userId));
        return response;
    }

    private void applyRequiredDetailsForVerification(DriverLicense license, DriverLicenseUpdateRequest request) {
        if (request != null) {
            license.setLicenseNumber(normalizeRequiredText(request.getLicenseNumber(), "License number"));
            license.setIssuingCountry(normalizeRequiredText(request.getIssuingCountry(), "Issuing country"));
            if (request.getExpiryDate() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expiry date cannot be empty");
            }
            license.setExpiryDate(request.getExpiryDate());
        }
    }

    private DriverLicenseResponse uploadImage(UUID userId, MultipartFile file, boolean front) {
        User user = findCurrentUser(userId);
        DriverLicense license = driverLicenseRepository.findByUser_Id(userId)
                .orElseGet(() -> createEmptyLicense(user));

        String storedUrl = fileStorageService.storeDriverLicenseImage(file, front ? "front" : "back");
        if (front) {
            license.setFrontImageUrl(storedUrl);
        } else {
            license.setBackImageUrl(storedUrl);
        }
        license.setUpdatedAt(LocalDateTime.now());
        clearVerificationState(license);

        DriverLicense saved = driverLicenseRepository.save(license);
        return toResponse(saved, userId);
    }

    private DriverLicense createEmptyLicense(User user) {
        DriverLicense license = new DriverLicense();
        license.setUser(user);
        license.setLicenseNumber("PENDING");
        license.setIssuingCountry("PENDING");
        license.setExpiryDate(LocalDate.of(2099, 1, 1));
        license.setVerified(false);
        license.setVerifiedAt(null);
        return driverLicenseRepository.save(license);
    }

    private void applyVerificationResult(DriverLicense license, boolean verified) {
        license.setVerified(verified);
        license.setVerifiedAt(verified ? LocalDateTime.now() : null);
        license.setUpdatedAt(LocalDateTime.now());
    }

    private void clearVerificationState(DriverLicense license) {
        license.setVerified(false);
        license.setVerifiedAt(null);
    }

    private User findCurrentUser(UUID userId) {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No authenticated tenant context found");
        }

        return userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private String normalizeRequiredText(String value, String fieldName) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " cannot be empty");
        }
        return trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank() || "PENDING".equalsIgnoreCase(value);
    }

    private DriverLicenseResponse toResponse(DriverLicense license, UUID userId) {
        DriverLicenseResponse response = new DriverLicenseResponse();
        if (license == null) {
            response.setUserId(userId);
            response.setVerified(false);
            return response;
        }
        response.setId(license.getId());
        response.setUserId(license.getUser() != null ? license.getUser().getId() : userId);
        response.setLicenseNumber(isBlank(license.getLicenseNumber()) ? "" : license.getLicenseNumber());
        response.setIssuingCountry(isBlank(license.getIssuingCountry()) ? "" : license.getIssuingCountry());
        response.setExpiryDate(isPendingExpiryDate(license.getExpiryDate()) ? null : license.getExpiryDate());
        response.setFrontImageUrl(license.getFrontImageUrl());
        response.setBackImageUrl(license.getBackImageUrl());
        response.setVerifiedAt(license.getVerifiedAt());
        response.setVerified(license.isVerified());
        return response;
    }

    private boolean isPendingExpiryDate(java.time.LocalDate expiryDate) {
        return expiryDate == null || LocalDate.of(2099, 1, 1).equals(expiryDate);
    }
}
