package com.wheelGo.controller;

import com.wheelGo.controller.support.SecuredControllerTestConfig;
import com.wheelGo.service.UserSettingsService;
import com.wheelGo.tools.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(useDefaultFilters = false)
@Import({UserSettingsController.class, SecuredControllerTestConfig.class})
class UserSettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserSettingsService userSettingsService;

    @Test
    void should_return_ok_when_change_password_valid() throws Exception {
        UUID userId = UUID.randomUUID();

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);

            mockMvc.perform(put("/api/user-settings/me/password")
                            .with(user("user").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "currentPassword":"OldPassword1",
                              "newPassword":"NewPassword1"
                            }
                            """))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void should_return_bad_request_when_change_password_invalid() throws Exception {
        mockMvc.perform(put("/api/user-settings/me/password")
                        .with(user("user").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword":"",
                                  "newPassword":""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
