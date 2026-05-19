package com.wheelGo.controller;

import com.wheelGo.controller.support.SecuredControllerTestConfig;
import com.wheelGo.model.user_profiles.UserProfileResponse;
import com.wheelGo.service.UserProfileService;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(useDefaultFilters = false)
@Import({UserProfileController.class, SecuredControllerTestConfig.class})
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserProfileService userProfileService;

    @Test
    void should_return_ok_when_get_my_profile() throws Exception {
        UUID userId = UUID.randomUUID();
        UserProfileResponse response = new UserProfileResponse();
        response.setUserId(userId);
        response.setFirstName("John");
        response.setLastName("Doe");
        when(userProfileService.getProfileByUserId(userId)).thenReturn(response);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);

            mockMvc.perform(get("/api/user-profile/me").with(user("user").roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$.lastName").value("Doe"));
        }
    }

    @Test
    void should_return_ok_when_update_my_profile() throws Exception {
        UUID userId = UUID.randomUUID();
        UserProfileResponse response = new UserProfileResponse();
        response.setUserId(userId);
        response.setPhone("123456");
        when(userProfileService.updateProfile(eq(userId), any())).thenReturn(response);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);

            mockMvc.perform(put("/api/user-profile/me")
                            .with(user("user").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "firstName":"John",
                                      "lastName":"Doe",
                                      "phone":"123456"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.phone").value("123456"));
        }
    }

    @Test
    void should_return_ok_when_upload_avatar() throws Exception {
        UUID userId = UUID.randomUUID();
        UserProfileResponse response = new UserProfileResponse();
        response.setUserId(userId);
        response.setAvatarUrl("/uploads/avatar.png");
        when(userProfileService.uploadAvatar(eq(userId), any())).thenReturn(response);

        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", MediaType.IMAGE_PNG_VALUE, "png".getBytes());

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);

            mockMvc.perform(multipart("/api/user-profile/me/avatar")
                            .file(file)
                            .with(user("user").roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.avatarUrl").value("/uploads/avatar.png"));
        }
    }

    @Test
    void should_return_not_found_when_profile_missing() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userProfileService.getProfileByUserId(userId)).thenThrow(new ResponseStatusException(NOT_FOUND, "Profile not found"));

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);

            mockMvc.perform(get("/api/user-profile/me").with(user("user").roles("USER")))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void should_return_unauthorized_when_not_authenticated() throws Exception {
        mockMvc.perform(get("/api/user-profile/me"))
                .andExpect(status().isUnauthorized());
    }
}
