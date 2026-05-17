package com.wheelGo.controller;

import com.wheelGo.controller.support.SecuredControllerTestConfig;
import com.wheelGo.model.enums.MaintenanceType;
import com.wheelGo.model.maintenance_records.MaintenanceRecordResponse;
import com.wheelGo.service.MaintenanceAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(useDefaultFilters = false)
@Import({MaintenanceAdminController.class, SecuredControllerTestConfig.class})
class MaintenanceAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MaintenanceAdminService maintenanceAdminService;

    @Test
    void should_return_ok_when_get_all_as_admin() throws Exception {
        MaintenanceRecordResponse response = new MaintenanceRecordResponse();
        response.setId(UUID.randomUUID());
        response.setVehicleName("BMW X5");
        response.setType(MaintenanceType.REPAIR);
        when(maintenanceAdminService.getAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/admin/maintenances").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].vehicleName").value("BMW X5"))
                .andExpect(jsonPath("$[0].type").value("REPAIR"));
    }

    @Test
    void should_return_ok_when_get_types_as_admin() throws Exception {
        when(maintenanceAdminService.getTypes()).thenReturn(List.of(MaintenanceType.REPAIR, MaintenanceType.INSPECTION));

        mockMvc.perform(get("/api/v1/admin/maintenances/types").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("REPAIR"));
    }

    @Test
    void should_return_created_when_create_valid() throws Exception {
        MaintenanceRecordResponse response = new MaintenanceRecordResponse();
        response.setId(UUID.randomUUID());
        response.setType(MaintenanceType.REPAIR);
        when(maintenanceAdminService.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/maintenances")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "vehicleId":"11111111-1111-1111-1111-111111111111",
                                  "type":"REPAIR",
                                  "description":"Brake work",
                                  "cost":100.00,
                                  "performedAt":"2026-05-17T10:00:00"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("REPAIR"));
    }

    @Test
    void should_return_unauthorized_when_not_authenticated() throws Exception {
        mockMvc.perform(get("/api/v1/admin/maintenances"))
                .andExpect(status().isUnauthorized());
    }
}
