package com.wheelGo.controller;

import com.wheelGo.model.addon.AddonResponse;
import com.wheelGo.service.AddonAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(useDefaultFilters = false)
@Import({AddonAdminController.class, AddonAdminControllerTest.TestSecurityConfig.class})
class AddonAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AddonAdminService addonAdminService;

    @Test
    void should_return_ok_when_get_all_authenticated() throws Exception {
        AddonResponse response = new AddonResponse();
        response.setId(UUID.randomUUID());
        response.setName("Bluetooth");
        response.setPrice(new BigDecimal("10.00"));
        when(addonAdminService.getAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/admin/addons").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].name").value("Bluetooth"));
    }

    @Test
    void should_return_unauthorized_when_not_authenticated() throws Exception {
        mockMvc.perform(get("/api/v1/admin/addons"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void should_return_forbidden_when_role_not_admin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/addons").with(user("user").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void should_return_ok_when_create_valid() throws Exception {
        AddonResponse response = new AddonResponse();
        response.setId(UUID.randomUUID());
        response.setName("Baby Seat");
        when(addonAdminService.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/addons")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Baby Seat",
                                  "price":25.00,
                                  "quantity":2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Baby Seat"));
    }

    @Test
    void should_return_not_found_when_delete_missing() throws Exception {
        UUID id = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new ResponseStatusException(NOT_FOUND, "Addon not found"))
                .when(addonAdminService).delete(id);

        mockMvc.perform(delete("/api/v1/admin/addons/{id}", id).with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Configuration
    @EnableWebSecurity
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(csrf -> csrf.disable())
                    .exceptionHandling(ex -> ex
                            .authenticationEntryPoint((request, response, authException) -> response.sendError(401))
                            .accessDeniedHandler((request, response, accessDeniedException) -> response.sendError(403))
                    )
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                            .anyRequest().authenticated()
                    )
                    .build();
        }
    }
}
