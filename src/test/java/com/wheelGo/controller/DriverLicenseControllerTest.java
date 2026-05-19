package com.wheelGo.controller;

import com.wheelGo.controller.support.SecuredControllerTestConfig;
import com.wheelGo.model.driver_licenses.DriverLicenseResponse;
import com.wheelGo.model.driver_licenses.DriverLicenseVerificationResponse;
import com.wheelGo.service.DriverLicenseService;
import com.wheelGo.tools.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(useDefaultFilters = false)
@Import({DriverLicenseController.class, SecuredControllerTestConfig.class})
class DriverLicenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DriverLicenseService driverLicenseService;

    @Test
    void should_return_ok_when_get_my_license() throws Exception {
        UUID userId = UUID.randomUUID();
        DriverLicenseResponse response = new DriverLicenseResponse();
        response.setUserId(userId);
        response.setLicenseNumber("DL123");
        when(driverLicenseService.getMyLicense(userId)).thenReturn(response);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);

            mockMvc.perform(get("/api/driver-license/me").with(user("user").roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.licenseNumber").value("DL123"));
        }
    }

    @Test
    void should_return_ok_when_update_my_license() throws Exception {
        UUID userId = UUID.randomUUID();
        DriverLicenseResponse response = new DriverLicenseResponse();
        response.setIssuingCountry("Kosovo");
        when(driverLicenseService.upsertMyLicense(eq(userId), any())).thenReturn(response);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);

            mockMvc.perform(put("/api/driver-license/me")
                            .with(user("user").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "licenseNumber":"DL123",
                                      "issuingCountry":"Kosovo",
                                      "expiryDate":"2028-01-01"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.issuingCountry").value("Kosovo"));
        }
    }

    @Test
    void should_return_ok_when_verify_with_json() throws Exception {
        UUID userId = UUID.randomUUID();
        DriverLicenseVerificationResponse response = new DriverLicenseVerificationResponse();
        response.setVerified(true);
        when(driverLicenseService.verifyMyLicense(eq(userId), any())).thenReturn(response);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);

            mockMvc.perform(post("/api/driver-license/me/verify")
                            .with(user("user").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "licenseNumber":"DL123",
                                      "issuingCountry":"Kosovo",
                                      "expiryDate":"2028-01-01"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.verified").value(true));
        }
    }

    @Test
    void should_return_ok_when_upload_front_image() throws Exception {
        UUID userId = UUID.randomUUID();
        DriverLicenseResponse response = new DriverLicenseResponse();
        response.setFrontImageUrl("/uploads/front.png");
        when(driverLicenseService.uploadFront(eq(userId), any())).thenReturn(response);

        MockMultipartFile file = new MockMultipartFile("file", "front.png", MediaType.IMAGE_PNG_VALUE, "png".getBytes());

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);

            mockMvc.perform(multipart("/api/driver-license/me/front-image")
                            .file(file)
                            .with(user("user").roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.frontImageUrl").value("/uploads/front.png"));
        }
    }
}
