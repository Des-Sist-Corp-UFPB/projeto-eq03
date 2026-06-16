package com.cristiane.salon.controllers;

import com.cristiane.salon.controller.AuditController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
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

}
