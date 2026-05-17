package com.wheelGo.controller;

import com.wheelGo.controller.support.SecuredControllerTestConfig;
import com.wheelGo.model.bookings.BookingResponse;
import com.wheelGo.service.BookingService;
import com.wheelGo.tools.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(useDefaultFilters = false)
@Import({BookingController.class, SecuredControllerTestConfig.class})
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    @Test
    void should_return_created_when_create_booking_valid() throws Exception {
        UUID userId = UUID.randomUUID();
        BookingResponse response = new BookingResponse();
        response.setId(UUID.randomUUID());
        response.setUserId(userId);
        response.setVehicleName("BMW X5");
        response.setStartDate(LocalDateTime.of(2026, 6, 1, 10, 0));

        when(bookingService.createBooking(eq(userId), any())).thenReturn(response);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);

            mockMvc.perform(post("/api/v1/bookings")
                            .with(user("user").roles("USER"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "vehicleId":"11111111-1111-1111-1111-111111111111",
                                      "startDate":"2026-06-01",
                                      "endDate":"2026-06-03"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.userId").value(userId.toString()))
                    .andExpect(jsonPath("$.vehicleName").value("BMW X5"));
        }
    }

    @Test
    void should_return_bad_request_when_create_booking_invalid() throws Exception {
        mockMvc.perform(post("/api/v1/bookings")
                        .with(user("user").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startDate":"2026-06-01",
                                  "endDate":"2026-06-03"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_ok_when_get_my_bookings() throws Exception {
        UUID userId = UUID.randomUUID();
        BookingResponse response = new BookingResponse();
        response.setId(UUID.randomUUID());
        response.setUserId(userId);
        response.setVehicleName("Audi A4");
        when(bookingService.getBookingsForUser(userId)).thenReturn(List.of(response));

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(userId);

            mockMvc.perform(get("/api/v1/bookings/me").with(user("user").roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].vehicleName").value("Audi A4"));
        }
    }

    @Test
    void should_return_unauthorized_when_not_authenticated() throws Exception {
        mockMvc.perform(get("/api/v1/bookings/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void should_return_forbidden_when_role_not_allowed() throws Exception {
        mockMvc.perform(get("/api/v1/bookings/me").with(user("guest").roles("GUEST")))
                .andExpect(status().isForbidden());
    }
}
