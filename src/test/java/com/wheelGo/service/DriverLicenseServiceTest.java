package com.wheelGo.service;

import com.wheelGo.model.driver_licenses.DriverLicense;
import com.wheelGo.model.driver_licenses.DriverLicenseResponse;
import com.wheelGo.model.driver_licenses.DriverLicenseUpdateRequest;
import com.wheelGo.model.driver_licenses.DriverLicenseVerificationResponse;
import com.wheelGo.model.tenant.Tenant;
import com.wheelGo.model.user.User;
import com.wheelGo.repository.DriverLicenseRepository;
import com.wheelGo.repository.UserRepository;
import com.wheelGo.schema.TenantSchemaExecutor;
import com.wheelGo.security.CustomUserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverLicenseServiceTest {

    @Mock private DriverLicenseRepository driverLicenseRepository;
    @Mock private UserRepository userRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private OllamaDriverLicenseVerificationService ollamaDriverLicenseVerificationService;
    @Mock private TenantSchemaExecutor tenantSchemaExecutor;
    @InjectMocks private DriverLicenseService driverLicenseService;

    private UUID userId;
    private UUID tenantId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        user = new User();
        user.setId(userId);
        user.setTenant(tenant);
        CustomUserPrincipal principal = new CustomUserPrincipal(userId, "user@example.com", "hash", "v1", "USER", tenantId, "tenant", false, null, null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void should_return_empty_license_response_when_license_missing() {
        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(user));
        when(driverLicenseRepository.findByUser_Id(userId)).thenReturn(Optional.empty());

        DriverLicenseResponse result = driverLicenseService.getMyLicense(userId);

        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.isVerified()).isFalse();
    }

    @Test
    void should_upsert_license_when_request_valid() {
        DriverLicenseUpdateRequest request = new DriverLicenseUpdateRequest();
        request.setLicenseNumber(" 123 ");
        request.setIssuingCountry(" Kosovo ");
        request.setExpiryDate(LocalDate.now().plusYears(1));
        DriverLicense license = new DriverLicense();
        license.setUser(user);

        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(user));
        when(driverLicenseRepository.findByUser_Id(userId)).thenReturn(Optional.of(license));
        when(driverLicenseRepository.save(license)).thenReturn(license);

        DriverLicenseResponse result = driverLicenseService.upsertMyLicense(userId, request);

        assertThat(result.getLicenseNumber()).isEqualTo("123");
        assertThat(result.getIssuingCountry()).isEqualTo("Kosovo");
    }

    @Test
    void should_upload_front_image_when_file_valid() {
        MockMultipartFile file = new MockMultipartFile("file", "front.png", "image/png", new byte[]{1});
        DriverLicense license = new DriverLicense();
        license.setUser(user);

        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(user));
        when(driverLicenseRepository.findByUser_Id(userId)).thenReturn(Optional.of(license));
        when(fileStorageService.storeDriverLicenseImage(file, "front")).thenReturn("/uploads/front.png");
        when(driverLicenseRepository.save(license)).thenReturn(license);

        DriverLicenseResponse result = driverLicenseService.uploadFront(userId, file);

        assertThat(result.getFrontImageUrl()).isEqualTo("/uploads/front.png");
    }

    @Test
    void should_throw_bad_request_when_verifying_without_both_images() {
        DriverLicense license = new DriverLicense();
        license.setUser(user);
        license.setLicenseNumber("123");
        license.setIssuingCountry("Kosovo");
        license.setExpiryDate(LocalDate.now().plusYears(1));

        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(user));
        when(driverLicenseRepository.findByUser_Id(userId)).thenReturn(Optional.of(license));

        assertThatThrownBy(() -> driverLicenseService.verifyMyLicense(userId, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Upload both front and back images");
    }

    @Test
    void should_verify_license_when_data_complete() {
        DriverLicense license = new DriverLicense();
        license.setUser(user);
        license.setLicenseNumber("123");
        license.setIssuingCountry("Kosovo");
        license.setExpiryDate(LocalDate.now().plusYears(1));
        license.setFrontImageUrl("/uploads/front.png");
        license.setBackImageUrl("/uploads/back.png");
        DriverLicenseVerificationResponse verification = new DriverLicenseVerificationResponse();
        verification.setVerified(true);

        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(user));
        when(driverLicenseRepository.findByUser_Id(userId)).thenReturn(Optional.of(license));
        when(driverLicenseRepository.save(any(DriverLicense.class))).thenReturn(license);
        when(fileStorageService.resolveStoredUpload("/uploads/front.png")).thenReturn(java.nio.file.Path.of("front.png"));
        when(fileStorageService.resolveStoredUpload("/uploads/back.png")).thenReturn(java.nio.file.Path.of("back.png"));
        when(ollamaDriverLicenseVerificationService.verifyAsync(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(verification));
        when(tenantSchemaExecutor.callInSchema(any(), any())).thenAnswer(invocation -> {
            java.util.function.Supplier<?> supplier = invocation.getArgument(1);
            return supplier.get();
        });

        DriverLicenseVerificationResponse result = driverLicenseService.verifyMyLicense(userId, null).join();

        assertThat(result.isVerified()).isTrue();
    }
}
