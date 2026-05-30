package com.wheelGo.service;

import com.wheelGo.model.driver_licenses.DriverLicense;
import com.wheelGo.model.driver_licenses.DriverLicenseResponse;
import com.wheelGo.model.driver_licenses.DriverLicenseUpdateRequest;
import com.wheelGo.model.driver_licenses.DriverLicenseVerificationResponse;
import com.wheelGo.model.user.User;
import com.wheelGo.model.user_profiles.UserProfile;
import com.wheelGo.repository.DriverLicenseRepository;
import com.wheelGo.repository.UserRepository;
import com.wheelGo.repository.UserProfileRepository;
import com.wheelGo.schema.TenantContext;
import com.wheelGo.schema.TenantSchemaExecutor;
import com.wheelGo.tools.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class DriverLicenseService {

    private final DriverLicenseRepository driverLicenseRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final OllamaDriverLicenseVerificationService ollamaDriverLicenseVerificationService;
    private final PaddleOcrDriverLicenseTextService paddleOcrDriverLicenseTextService;
    private final TenantSchemaExecutor tenantSchemaExecutor;
    private final UserProfileRepository userProfileRepository;

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
    public CompletableFuture<DriverLicenseVerificationResponse> verifyMyLicense(UUID userId, DriverLicenseUpdateRequest request) {
        return verifyMyLicenseInternal(userId, request, null, null);
    }

    @Transactional
    public CompletableFuture<DriverLicenseVerificationResponse> verifyMyLicense(UUID userId,
                                                                                DriverLicenseUpdateRequest request,
                                                                                MultipartFile frontImage,
                                                                                MultipartFile backImage) {
        return verifyMyLicenseInternal(userId, request, frontImage, backImage);
    }

    private CompletableFuture<DriverLicenseVerificationResponse> verifyMyLicenseInternal(UUID userId,
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
        String schemaName = TenantContext.getCurrentSchema();
        String expectedProfileName = userProfileRepository.findByUser_Id(userId)
                .map(this::buildProfileName)
                .orElse("");
        java.nio.file.Path frontImagePath = fileStorageService.resolveStoredUpload(license.getFrontImageUrl());
        java.nio.file.Path backImagePath = fileStorageService.resolveStoredUpload(license.getBackImageUrl());

        PaddleOcrDriverLicenseTextService.OcrResult ocrResult = paddleOcrDriverLicenseTextService.readText(frontImagePath, backImagePath);

        return ollamaDriverLicenseVerificationService.verifyAsync(frontImagePath, backImagePath)
                .thenApply(aiResponse -> tenantSchemaExecutor.callInSchema(schemaName, () -> {
                    DriverLicenseVerificationResponse response = buildOcrVerificationResponse(
                            aiResponse,
                            ocrResult,
                            license,
                            expectedProfileName
                    );
                    applyVerificationResult(license, response.isVerified());
                    DriverLicense saved = driverLicenseRepository.save(license);
                    response.setLicense(toResponse(saved, userId));
                    return response;
                }));
    }

    private DriverLicenseVerificationResponse buildOcrVerificationResponse(DriverLicenseVerificationResponse aiResponse,
                                                                           PaddleOcrDriverLicenseTextService.OcrResult ocrResult,
                                                                           DriverLicense license,
                                                                           String expectedProfileName) {
        DriverLicenseVerificationResponse response = new DriverLicenseVerificationResponse();
        String normalizedText = normalizeForContains(ocrResult.getText());
        boolean documentLooksLikeLicense = aiResponse.isDocumentVisible() && aiResponse.isDriverLicenseLike();
        boolean licenseNumberMatches = containsNormalized(normalizedText, license.getLicenseNumber());
        boolean countryMatches = countryMatches(normalizedText, license.getIssuingCountry());
        boolean expiryDateMatches = expiryDateMatches(normalizedText, license.getExpiryDate());
        boolean nameMatches = isBlank(expectedProfileName) || nameMatches(normalizedText, expectedProfileName);

        response.setExtractedLicenseNumber(licenseNumberMatches ? license.getLicenseNumber() : "");
        response.setExtractedCountry(countryMatches ? license.getIssuingCountry() : "");
        response.setExtractedExpiryDate(expiryDateMatches ? license.getExpiryDate().toString() : "");
        response.setExtractedName(nameMatches && !isBlank(expectedProfileName) ? expectedProfileName : "");
        response.setLicenseNumberMatches(licenseNumberMatches);
        response.setIssuingCountryMatches(countryMatches);
        response.setExpiryDateMatches(expiryDateMatches);
        response.setProfileNameMatches(nameMatches);
        response.setRequiredFieldsExtracted(licenseNumberMatches && countryMatches && expiryDateMatches);
        response.setOcrText(ocrResult.getText());
        response.setOcrLines(ocrResult.getLines());

        boolean ocrMatches = licenseNumberMatches && countryMatches && expiryDateMatches && nameMatches;
        response.setVerified(documentLooksLikeLicense && ocrMatches);
        response.setVerdict(ocrMatches ? "ocr_passed" : "rejected");
        response.setDocumentVisible(aiResponse.isDocumentVisible());
        response.setImageQualityOk(aiResponse.isImageQualityOk());
        response.setDriverLicenseLike(aiResponse.isDriverLicenseLike());
        response.setTamperingSignals(aiResponse.getTamperingSignals() == null ? List.of() : aiResponse.getTamperingSignals());
        response.setConfidence(calculateOcrConfidence(licenseNumberMatches, countryMatches, expiryDateMatches, nameMatches, expectedProfileName));
        if (!documentLooksLikeLicense) {
            response.setVerdict("rejected");
            response.setRecommendation("AI rejected the upload because it does not look like a driver license.");
        } else if (!ocrMatches) {
            List<String> mismatches = new ArrayList<>();
            if (!licenseNumberMatches) {
                mismatches.add("license number");
            }
            if (!countryMatches) {
                mismatches.add("issuing country");
            }
            if (!expiryDateMatches) {
                mismatches.add("expiry date");
            }
            if (!nameMatches) {
                mismatches.add("profile name");
            }
            response.setRecommendation("OCR mismatch: " + String.join(", ", mismatches) + " did not match the uploaded images.");
        } else {
            response.setRecommendation("The upload looks like a driver license and OCR matched the submitted details.");
        }
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

    private String buildProfileName(UserProfile profile) {
        return ((profile.getFirstName() == null ? "" : profile.getFirstName()) + " " +
                (profile.getLastName() == null ? "" : profile.getLastName())).trim();
    }

    private boolean containsNormalized(String normalizedText, String expectedValue) {
        String normalizedExpected = normalizeForContains(expectedValue);
        return !normalizedExpected.isBlank() && normalizedText.contains(normalizedExpected);
    }

    private boolean countryMatches(String normalizedText, String issuingCountry) {
        if (containsNormalized(normalizedText, issuingCountry)) {
            return true;
        }
        String normalizedCountry = normalizeForContains(issuingCountry);
        if (List.of("kosovo", "kosova", "kosove").contains(normalizedCountry)) {
            return normalizedText.contains("kosovo") || normalizedText.contains("kosova") || normalizedText.contains("kosove");
        }
        return false;
    }

    private boolean expiryDateMatches(String normalizedText, LocalDate expiryDate) {
        if (expiryDate == null) {
            return false;
        }
        return expiryDateVariants(expiryDate).stream()
                .map(this::normalizeForContains)
                .anyMatch(normalizedText::contains);
    }

    private List<String> expiryDateVariants(LocalDate date) {
        return List.of(
                date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                date.format(DateTimeFormatter.ofPattern("dd.MM.yy")),
                date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                date.format(DateTimeFormatter.ofPattern("dd/MM/yy")),
                date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
                date.format(DateTimeFormatter.ofPattern("dd-MM-yy")),
                date.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")),
                date.format(DateTimeFormatter.ofPattern("MM/dd/yy")),
                date.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                date.format(DateTimeFormatter.ofPattern("ddMMyyyy")),
                date.format(DateTimeFormatter.ofPattern("ddMMyy"))
        );
    }

    private boolean nameMatches(String normalizedText, String expectedProfileName) {
        String[] parts = expectedProfileName.trim().split("\\s+");
        int matchedParts = 0;
        for (String part : parts) {
            String normalizedPart = normalizeForContains(part);
            if (normalizedPart.length() >= 2 && normalizedText.contains(normalizedPart)) {
                matchedParts++;
            }
        }
        return matchedParts >= Math.min(2, parts.length);
    }

    private String normalizeForContains(String value) {
        if (value == null) {
            return "";
        }
        String withoutMarks = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutMarks.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private double calculateOcrConfidence(boolean licenseNumberMatches,
                                          boolean countryMatches,
                                          boolean expiryDateMatches,
                                          boolean nameMatches,
                                          String expectedProfileName) {
        int total = isBlank(expectedProfileName) ? 3 : 4;
        int matched = 0;
        if (licenseNumberMatches) {
            matched++;
        }
        if (countryMatches) {
            matched++;
        }
        if (expiryDateMatches) {
            matched++;
        }
        if (!isBlank(expectedProfileName) && nameMatches) {
            matched++;
        }
        return (double) matched / total;
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
