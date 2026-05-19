package com.wheelGo.controller;

import com.wheelGo.model.tenant.Tenant;
import com.wheelGo.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(useDefaultFilters = false)
@Import(PublicTenantController.class)
class PublicTenantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TenantRepository tenantRepository;

    @Test
    void should_return_ok_when_slug_exists() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setName("WheelGo");
        tenant.setSlug("wheelgo");
        when(tenantRepository.findBySlug("wheelgo")).thenReturn(Optional.of(tenant));

        mockMvc.perform(get("/api/public/tenants/wheelgo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("WheelGo"))
                .andExpect(jsonPath("$.slug").value("wheelgo"));
    }

    @Test
    void should_return_forbidden_when_slug_reserved() throws Exception {
        mockMvc.perform(get("/api/public/tenants/super-admin-tenant"))
                .andExpect(status().isForbidden());
    }

    @Test
    void should_return_not_found_when_slug_missing() throws Exception {
        when(tenantRepository.findBySlug("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/public/tenants/missing"))
                .andExpect(status().isNotFound());
    }
}
