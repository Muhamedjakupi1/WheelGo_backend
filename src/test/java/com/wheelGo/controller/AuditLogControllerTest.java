package com.wheelGo.controller;

import com.wheelGo.controller.support.SecuredControllerTestConfig;
import com.wheelGo.model.audit_logs.AuditLogResponse;
import com.wheelGo.model.enums.AuditAction;
import com.wheelGo.service.AuditLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(useDefaultFilters = false)
@Import({AuditLogController.class, SecuredControllerTestConfig.class})
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditLogService auditLogService;

    @Test
    void should_return_ok_when_get_all_as_admin() throws Exception {
        AuditLogResponse response = new AuditLogResponse();
        response.setId(UUID.randomUUID());
        response.setAction(AuditAction.LOGIN);
        when(auditLogService.getAllAuditLogs()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/audit-logs").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].action").value("LOGIN"));
    }

    @Test
    void should_return_ok_when_get_by_user_email() throws Exception {
        when(auditLogService.getAuditLogsByUserEmail("admin@example.com")).thenReturn(List.of(new AuditLogResponse()));

        mockMvc.perform(get("/api/v1/audit-logs/user")
                        .with(user("admin").roles("ADMIN"))
                        .param("email", "admin@example.com"))
                .andExpect(status().isOk());
    }

    @Test
    void should_return_forbidden_when_not_admin() throws Exception {
        mockMvc.perform(get("/api/v1/audit-logs").with(user("user").roles("USER")))
                .andExpect(status().isForbidden());
    }
}
