package com.cristiane.salon.models.ai.controller;

import com.cristiane.salon.controllers.BaseControllerTest;
import com.cristiane.salon.models.ai.dto.McpTokenGeneratedResponse;
import com.cristiane.salon.models.ai.dto.McpTokenResponse;
import com.cristiane.salon.models.ai.service.McpTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(McpTokenController.class)
class McpTokenControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private McpTokenService mcpTokenService;

    @Test
    @WithMockUser(roles = { "SYSADMIN" })
    void list_returnsTokens() throws Exception {
        McpTokenResponse token = new McpTokenResponse(1L, "T", "sysadmin@salao.com", LocalDateTime.now(), null, null, false);
        when(mcpTokenService.list()).thenReturn(List.of(token));

        mvc.perform(get("/v1/sysadmin/ai-config/tokens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("T"));
    }

    @Test
    @WithMockUser(roles = { "SYSADMIN" })
    void generate_returns201WithRawValue() throws Exception {
        McpTokenResponse token = new McpTokenResponse(1L, "Claude Desktop", "sysadmin@salao.com", LocalDateTime.now(), null, null, false);
        when(mcpTokenService.generate(any())).thenReturn(new McpTokenGeneratedResponse(token, "mcp_rawvalue123"));

        String body = "{\"name\":\"Claude Desktop\"}";

        mvc.perform(post("/v1/sysadmin/ai-config/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rawValue").value("mcp_rawvalue123"))
                .andExpect(jsonPath("$.token.name").value("Claude Desktop"));
    }

    @Test
    @WithMockUser(roles = { "SYSADMIN" })
    void generate_returns400_whenNameMissing() throws Exception {
        mvc.perform(post("/v1/sysadmin/ai-config/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = { "SYSADMIN" })
    void revoke_returns204() throws Exception {
        doNothing().when(mcpTokenService).revoke(eq(1L));

        mvc.perform(delete("/v1/sysadmin/ai-config/tokens/1"))
                .andExpect(status().isNoContent());
    }
}
