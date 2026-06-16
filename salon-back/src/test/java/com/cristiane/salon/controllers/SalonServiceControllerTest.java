package com.cristiane.salon.controllers;

import com.cristiane.salon.controller.SalonServiceController;
import com.cristiane.salon.models.service.service.SalonServiceManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import com.cristiane.salon.models.service.dto.SalonServiceResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SalonServiceController.class)
class SalonServiceControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SalonServiceManager salonServiceManager;

    @Test
    @WithMockUser
    void createReturns201_whenValid() throws Exception {
        when(salonServiceManager.create(any())).thenReturn(null);

        String body = "{\"name\":\"cut\",\"durationMin\":30}";

        mvc.perform(post("/v1/services")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser
    void createReturns400_whenInvalid() throws Exception {
        String body = "{}";

        mvc.perform(post("/v1/services")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void reactivateReturns200() throws Exception {
        SalonServiceResponse dummyResponse = new SalonServiceResponse(
                1L, "cut", "description", new BigDecimal("50.0"), 30, "30 min", true
        );
        when(salonServiceManager.reactivate(any())).thenReturn(dummyResponse);

        mvc.perform(patch("/v1/services/1/reactivate"))
                .andExpect(status().isOk());
    }
}

