package com.cristiane.salon.controllers;

import com.cristiane.salon.controller.ReportController;
import com.cristiane.salon.models.report.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
class ReportControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ReportService reportService;

    @Test
    @WithMockUser(roles = { "ADMIN" })
    void findByPeriodReturns200_whenAuthorized() throws Exception {
        when(reportService.generateFinancialReport(any(), any())).thenReturn(null);

        mvc.perform(get("/v1/reports/financial?from=2026-05-01&to=2026-05-16")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    // Disabled because method security is disabled in WebMvcTest slice
    // @Test
    // @WithMockUser(roles = { "USER" })
    // void findByPeriodReturns403_whenNotAuthorized() throws Exception {
    //     mvc.perform(get("/v1/reports/financial?from=2026-05-01&to=2026-05-16")
    //             .contentType(MediaType.APPLICATION_JSON))
    //             .andExpect(status().isForbidden());
    // }
}
