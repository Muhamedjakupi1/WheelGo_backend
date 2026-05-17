package com.wheelGo.controller;

import com.wheelGo.controller.support.SecuredControllerTestConfig;
import com.wheelGo.model.vehicles.VehicleResponse;
import com.wheelGo.service.VehicleAdminService;
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

import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(useDefaultFilters = false)
@Import({VehicleController.class, SecuredControllerTestConfig.class})
class VehicleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VehicleAdminService vehicleAdminService;

    @Test
    void should_return_ok_when_get_all_authenticated() throws Exception {
        VehicleResponse response = new VehicleResponse();
        response.setId(UUID.randomUUID());
        response.setMake("BMW");
        response.setModel("X5");
        when(vehicleAdminService.getAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/vehicles").with(user("user").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].make").value("BMW"))
                .andExpect(jsonPath("$[0].model").value("X5"));
    }

    @Test
    void should_return_ok_when_get_by_id_exists() throws Exception {
        UUID id = UUID.randomUUID();
        VehicleResponse response = new VehicleResponse();
        response.setId(id);
        response.setMake("Audi");
        when(vehicleAdminService.getById(id)).thenReturn(response);

        mockMvc.perform(get("/api/v1/vehicles/{id}", id).with(user("user").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.make").value("Audi"));
    }

    @Test
    void should_return_not_found_when_vehicle_missing() throws Exception {
        UUID id = UUID.randomUUID();
        when(vehicleAdminService.getById(id)).thenThrow(new ResponseStatusException(NOT_FOUND, "Vehicle not found"));

        mockMvc.perform(get("/api/v1/vehicles/{id}", id).with(user("user").roles("USER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_return_unauthorized_when_not_authenticated() throws Exception {
        mockMvc.perform(get("/api/v1/vehicles"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void should_return_forbidden_when_role_not_allowed() throws Exception {
        mockMvc.perform(get("/api/v1/vehicles").with(user("guest").roles("GUEST")))
                .andExpect(status().isForbidden());
    }
}
