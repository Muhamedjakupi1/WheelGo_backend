package com.wheelGo.controller;

import com.wheelGo.controller.support.SecuredControllerTestConfig;
import com.wheelGo.model.tenant_settings.SupportedCurrencyResponse;
import com.wheelGo.model.tenant_settings.TenantSettingsResponse;
import com.wheelGo.service.SupportedCurrencyService;
import com.wheelGo.service.TenantAdminSettingsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(useDefaultFilters = false)
@Import({TenantAdminSettingsController.class, SecuredControllerTestConfig.class})
class TenantAdminSettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TenantAdminSettingsService tenantAdminSettingsService;

    @MockitoBean
    private SupportedCurrencyService supportedCurrencyService;

    @Test
    void should_return_ok_when_get_current_tenant_settings_as_admin() throws Exception {
        TenantSettingsResponse response = new TenantSettingsResponse();
        response.setCurrency("EUR");
        when(tenantAdminSettingsService.getCurrentTenantSettings()).thenReturn(response);

        mockMvc.perform(get("/api/v1/admin/tenant-settings").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("EUR"));
    }

    @Test
    void should_return_ok_when_get_supported_currencies_as_admin() throws Exception {
        when(supportedCurrencyService.getSupportedCurrencies())
                .thenReturn(List.of(new SupportedCurrencyResponse("EUR", "Euro", "€")));

        mockMvc.perform(get("/api/v1/admin/tenant-settings/currencies").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("EUR"));
    }

    @Test
    void should_return_forbidden_when_not_admin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/tenant-settings").with(user("user").roles("USER")))
                .andExpect(status().isForbidden());
    }
}
