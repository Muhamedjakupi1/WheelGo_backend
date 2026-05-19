package com.wheelGo.controller;

import com.wheelGo.controller.support.SecuredControllerTestConfig;
import com.wheelGo.model.bookings.BookingResponse;
import com.wheelGo.service.BookingService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(useDefaultFilters = false)
@Import({BookingAdminController.class, SecuredControllerTestConfig.class})
class BookingAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    @Test
    void should_return_ok_when_get_all_as_admin() throws Exception {
        BookingResponse response = new BookingResponse();
        response.setId(UUID.randomUUID());
        response.setVehicleName("BMW X5");
        when(bookingService.getBookingsForAdmin()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/admin/bookings").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vehicleName").value("BMW X5"));
    }

    @Test
    void should_return_ok_when_confirm_valid() throws Exception {
        UUID id = UUID.randomUUID();
        BookingResponse response = new BookingResponse();
        response.setId(id);
        when(bookingService.confirmBooking(any(), any())).thenReturn(response);

        mockMvc.perform(patch("/api/v1/admin/bookings/{id}/confirm", id)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "addonCharge":10.00,
                                  "addonName":"Baby Seat"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void should_return_not_found_when_delete_missing() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ResponseStatusException(NOT_FOUND, "Booking not found")).when(bookingService).deleteBookingAsAdmin(id);

        mockMvc.perform(delete("/api/v1/admin/bookings/{id}", id).with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_return_forbidden_when_not_admin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/bookings").with(user("user").roles("USER")))
                .andExpect(status().isForbidden());
    }
}
