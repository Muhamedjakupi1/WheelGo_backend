package com.wheelGo.controller;

import com.wheelGo.controller.support.SecuredControllerTestConfig;
import com.wheelGo.model.user.UserResponse;
import com.wheelGo.model.enums.Role;
import com.wheelGo.service.UserAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(useDefaultFilters = false)
@Import({UserAdminController.class, SecuredControllerTestConfig.class})
class UserAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserAdminService userAdminService;

    @Test
    void should_return_ok_when_get_all_as_admin() throws Exception {
        UserResponse response = new UserResponse();
        response.setId(UUID.randomUUID());
        response.setEmail("user@example.com");
        response.setRole(Role.USER);
        when(userAdminService.getAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/admin/users").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].email").value("user@example.com"));
    }

    @Test
    void should_return_not_found_when_get_by_id_missing() throws Exception {
        UUID id = UUID.randomUUID();
        when(userAdminService.getById(id)).thenThrow(new ResponseStatusException(NOT_FOUND, "User not found"));

        mockMvc.perform(get("/api/v1/admin/users/{id}", id).with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_return_bad_request_when_update_invalid() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(patch("/api/v1/admin/users/{id}", id)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"invalid-email",
                                  "password":"123"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_no_content_when_delete_existing() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/admin/users/{id}", id).with(user("admin").roles("ADMIN")))
                .andExpect(status().isNoContent());
    }

    @Test
    void should_return_unauthorized_when_not_authenticated() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void should_return_forbidden_when_not_admin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users").with(user("user").roles("USER")))
                .andExpect(status().isForbidden());
    }
}
