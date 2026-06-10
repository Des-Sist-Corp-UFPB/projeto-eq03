package com.cristiane.salon.controllers;

import com.cristiane.salon.models.appointment.service.AppointmentService;
import com.cristiane.salon.models.cashflow.service.CashFlowService;
import com.cristiane.salon.security.VerifyUserPermissions;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class ErrorScenariosTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private CashFlowService cashFlowService;

    @Autowired
    private VerifyUserPermissions verifyUserPermissions;

    @Test
    void whenInvalidAppointment_thenReturns400() throws Exception {
        String invalidJson = "{}";

        mvc.perform(post("/v1/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = { "USER" })
    void whenForbiddenOnCashFlow_thenReturns403() throws Exception {
        when(verifyUserPermissions.userOwnResourceOrHasPermission(null)).thenReturn(false);

        String body = "{\"type\":\"INCOME\",\"amount\":100.0,\"description\":\"test\",\"date\":\"2026-05-16\"}";

        mvc.perform(post("/v1/cashflow")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void whenServiceThrows_thenReturns500() throws Exception {
        when(appointmentService.create(any())).thenThrow(new RuntimeException("boom"));

        String body = "{\"employeeId\":1,\"serviceId\":1}";

        mvc.perform(post("/v1/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser
    void whenMissingRequestParam_thenReturns400() throws Exception {
        // PATCH without 'status' request param should return 400
        when(verifyUserPermissions.userOwnResourceOrHasPermission(null)).thenReturn(true);
        mvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/v1/appointments/1/status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Disabled("Ignorado temporariamente devido a conflitos de infraestrutura no Spring Test")
    void whenUnknownEndpoint_thenReturns404() throws Exception {
        mvc.perform(post("/v1/this-path-does-not-exist")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public AppointmentService appointmentService() {
            return Mockito.mock(AppointmentService.class);
        }

        @Bean
        public CashFlowService cashFlowService() {
            return Mockito.mock(CashFlowService.class);
        }

        @Bean
        public VerifyUserPermissions verifyUserPermissions() {
            return Mockito.mock(VerifyUserPermissions.class);
        }

        @Bean
        public MockMvc mockMvc(WebApplicationContext wac) {
            return MockMvcBuilders.webAppContextSetup(wac).build();
        }
    }
}
