package com.wheelGo.controller;

import com.wheelGo.controller.support.SecuredControllerTestConfig;
import com.wheelGo.model.addon.Addon;
import com.wheelGo.repository.AddonRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(useDefaultFilters = false)
@Import({AddonController.class, SecuredControllerTestConfig.class})
class AddonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AddonRepository addonRepository;

    @Test
    void should_return_ok_when_get_active_addons_authenticated() throws Exception {
        Addon addon = new Addon();
        addon.setId(UUID.randomUUID());
        addon.setName("Baby Seat");
        addon.setPrice(new BigDecimal("15.00"));
        addon.setIsActive(true);
        addon.setIsDeleted(false);
        when(addonRepository.findAllByIsActiveTrueAndIsDeletedFalseOrderByNameAsc()).thenReturn(List.of(addon));

        mockMvc.perform(get("/api/v1/addons").with(user("user").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].name").value("Baby Seat"));
    }

    @Test
    void should_return_unauthorized_when_not_authenticated() throws Exception {
        mockMvc.perform(get("/api/v1/addons"))
                .andExpect(status().isUnauthorized());
    }
}
