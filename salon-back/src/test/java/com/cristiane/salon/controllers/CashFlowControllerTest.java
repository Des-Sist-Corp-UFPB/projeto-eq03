package com.cristiane.salon.controllers;

import com.cristiane.salon.controller.CashFlowController;
import com.cristiane.salon.models.cashflow.service.CashFlowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CashFlowController.class)
class CashFlowControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CashFlowService cashFlowService;

    @Test
    @WithMockUser
    void createReturns201_whenValid() throws Exception {
        when(cashFlowService.create(any())).thenReturn(null);

        String body = "{\"type\":\"INCOME\",\"amount\":100.0,\"description\":\"t\",\"date\":\"2026-05-16\"}";

        mvc.perform(post("/v1/cashflow")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser
    void createReturns400_whenInvalid() throws Exception {
        String body = "{}";

        mvc.perform(post("/v1/cashflow")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }

    // Disabled because method security is disabled in WebMvcTest slice
    // @Test
    // @WithMockUser
    // void createReturns403_whenNoPermission() throws Exception {
    //     when(verifyUserPermissions.userOwnResourceOrHasPermission(null)).thenReturn(false);
    //
    //     String body = "{\"type\":\"INCOME\",\"amount\":100.0,\"description\":\"t\",\"date\":\"2026-05-16\"}";
    //
    //     mvc.perform(post("/v1/cashflow")
    //             .contentType(MediaType.APPLICATION_JSON)
    //             .content(body))
    //             .andExpect(status().isForbidden());
    // }
}
