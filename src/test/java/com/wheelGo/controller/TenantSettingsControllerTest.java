package com.wheelGo.controller;

import com.wheelGo.controller.support.SecuredControllerTestConfig;
import com.wheelGo.model.tenant_settings.TenantSettingsResponse;
import com.wheelGo.service.TenantAdminSettingsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(useDefaultFilters = false)
@Import({TenantSettingsController.class, SecuredControllerTestConfig.class})
class TenantSettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TenantAdminSettingsService tenantAdminSettingsService;

    @Test
    void should_return_ok_when_get_current_tenant_settings() throws Exception {
        TenantSettingsResponse response = new TenantSettingsResponse();
        response.setCurrency("EUR");
        when(tenantAdminSettingsService.getCurrentTenantSettings()).thenReturn(response);

        mockMvc.perform(get("/api/v1/tenant-settings").with(user("user").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("EUR"));
    }

    @Test
    void should_return_unauthorized_when_not_authenticated() throws Exception {
        mockMvc.perform(get("/api/v1/tenant-settings"))
                .andExpect(status().isUnauthorized());
    }
}
