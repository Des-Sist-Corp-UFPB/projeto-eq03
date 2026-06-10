package com.cristiane.salon.controllers;

import com.cristiane.salon.models.report.service.ReportService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class ReportControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ReportService reportService;

    @Autowired
    private com.cristiane.salon.security.VerifyUserPermissions verifyUserPermissions;

    @Test
    @WithMockUser(roles = { "ADMIN" })
    void findByPeriodReturns200_whenAuthorized() throws Exception {
        when(verifyUserPermissions.userOwnResourceOrHasPermission(null)).thenReturn(true);
        when(reportService.generateFinancialReport(any(), any())).thenReturn(null);

        mvc.perform(get("/v1/reports/financial?from=2026-05-01&to=2026-05-16")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = { "USER" })
    void findByPeriodReturns403_whenNotAuthorized() throws Exception {
        mvc.perform(get("/v1/reports/financial?from=2026-05-01&to=2026-05-16")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public ReportService reportService() {
            return Mockito.mock(ReportService.class);
        }

        @Bean
        public com.cristiane.salon.security.VerifyUserPermissions verifyUserPermissions() {
            return Mockito.mock(com.cristiane.salon.security.VerifyUserPermissions.class);
        }

        @Bean
        public MockMvc mockMvc(WebApplicationContext wac) {
            return MockMvcBuilders.webAppContextSetup(wac).build();
        }
    }
}
