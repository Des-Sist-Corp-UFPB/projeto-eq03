package com.cristiane.salon.controllers;

import com.cristiane.salon.controller.ReportController;
import com.cristiane.salon.models.report.dto.AppointmentReportResponse;
import com.cristiane.salon.models.report.dto.FinancialReportResponse;
import com.cristiane.salon.models.report.dto.PayrollReportResponse;
import com.cristiane.salon.models.report.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
        FinancialReportResponse response = new FinancialReportResponse(
                BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.TEN, List.of(), "Period"
        );
        when(reportService.generateFinancialReport(any(), any())).thenReturn(response);

        mvc.perform(get("/v1/reports/financial?from=2026-05-01&to=2026-05-16")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("Period"));
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    void getPayrollReportReturns200() throws Exception {
        PayrollReportResponse response = new PayrollReportResponse(List.of(), "Period");
        when(reportService.generatePayrollReport(any(LocalDate.class), any(LocalDate.class))).thenReturn(response);

        mvc.perform(get("/v1/reports/payroll?from=2026-05-01&to=2026-05-16")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("Period"));
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    void getAppointmentReportReturns200() throws Exception {
        AppointmentReportResponse response = new AppointmentReportResponse(0L, 0L, 0L, 0L, 0L, Map.of(), Map.of(), "Period");
        when(reportService.generateAppointmentReport(any(), any())).thenReturn(response);

        mvc.perform(get("/v1/reports/appointments?from=2026-05-01&to=2026-05-16")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("Period"));
    }
}
