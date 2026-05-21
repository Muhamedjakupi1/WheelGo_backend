package com.wheelGo.controller;

import com.wheelGo.config.SecurityConfig;
import com.wheelGo.config.TenantSchemaFilter;
import com.wheelGo.controller.support.SecuredControllerTestConfig;
import com.wheelGo.security.JwtAuthenticationFilter;
import com.wheelGo.security.RestAccessDeniedHandler;
import com.wheelGo.security.RestAuthenticationEntryPoint;
import com.wheelGo.security.SwaggerDevAuthenticationFilter;
import com.wheelGo.auth.AuthResponse;
import com.wheelGo.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = AuthController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = TenantSchemaFilter.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SwaggerDevAuthenticationFilter.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RestAuthenticationEntryPoint.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RestAccessDeniedHandler.class)
        }
)
@Import(SecuredControllerTestConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void should_return_ok_when_login_valid() throws Exception {
        AuthResponse response = new AuthResponse(
                "token", "user@example.com", "USER", UUID.randomUUID(), UUID.randomUUID(), "tenant", false, null, null
        );
        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/login/tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"user@example.com",
                                  "password":"Password1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("token"))
                .andExpect(jsonPath("$.email").value("user@example.com"));
    }

    @Test
    void should_return_forbidden_when_signup_rejected_by_service() throws Exception {
        when(authService.signup(any())).thenThrow(new ResponseStatusException(FORBIDDEN, "Public signup is disabled for this tenant"));

        mockMvc.perform(post("/api/auth/signup/tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"user@example.com",
                                  "password":"Password1",
                                  "firstName":"John",
                                  "lastName":"Doe",
                                  "phone":"123"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Public signup is disabled for this tenant"))
                .andExpect(jsonPath("$.path").value("/api/auth/signup/tenant"));
    }

    @Test
    void should_return_bad_request_when_signup_runtime_exception_occurs() throws Exception {
        when(authService.signup(any())).thenThrow(new RuntimeException("Email is required"));

        mockMvc.perform(post("/api/auth/signup/tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"",
                                  "password":"Password1",
                                  "firstName":"John",
                                  "lastName":"Doe",
                                  "phone":"123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email is required"));
    }
}
