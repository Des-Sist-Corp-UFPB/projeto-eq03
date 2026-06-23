package com.cristiane.salon.models.audit.controller;

import com.cristiane.salon.controllers.BaseControllerTest;

import com.cristiane.salon.models.audit.controller.AuditController;
import com.cristiane.salon.models.audit.AuditLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuditController.class)
class AuditControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @WithMockUser(roles = "SYSADMIN")
    void getAllAuditLogsReturns200_whenSysadmin() throws Exception {
        when(auditLogService.getAuditLogsWithCombinedFilters(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        mvc.perform(get("/v1/audit")
                .param("userId", "1")
                .param("action", "CREATE")
                .param("entityType", "Product")
                .param("startDate", "2026-06-16")
                .param("endDate", "2026-06-16"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SYSADMIN")
    void getAuditLogsByUserReturns200() throws Exception {
        AuditLog log = new AuditLog();
        log.setId(1L);
        when(auditLogService.getAuditLogsByUser(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(log)));

        mvc.perform(get("/v1/audit/user/1")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SYSADMIN")
    void getAuditLogsByActionReturns200() throws Exception {
        AuditLog log = new AuditLog();
        log.setId(2L);
        when(auditLogService.getAuditLogsByAction(eq("CREATE"), any()))
                .thenReturn(new PageImpl<>(List.of(log)));

        mvc.perform(get("/v1/audit/action/CREATE")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SYSADMIN")
    void getAuditLogsByEntityTypeReturns200() throws Exception {
        AuditLog log = new AuditLog();
        log.setId(3L);
        when(auditLogService.getAuditLogsByEntityType(eq("PRODUCT"), any()))
                .thenReturn(new PageImpl<>(List.of(log)));

        mvc.perform(get("/v1/audit/entity/PRODUCT")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "SYSADMIN")
    void getAuditLogsByDateRangeReturns200() throws Exception {
        AuditLog log = new AuditLog();
        log.setId(4L);
        when(auditLogService.getAuditLogsByDateRange(any(LocalDateTime.class), any(LocalDateTime.class), any()))
                .thenReturn(new PageImpl<>(List.of(log)));

        mvc.perform(get("/v1/audit/range")
                .param("from", "2026-06-16T00:00:00")
                .param("to", "2026-06-16T23:59:59")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
