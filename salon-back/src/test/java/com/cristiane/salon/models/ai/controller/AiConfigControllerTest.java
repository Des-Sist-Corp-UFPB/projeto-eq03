package com.cristiane.salon.models.ai.controller;

import com.cristiane.salon.controllers.BaseControllerTest;
import com.cristiane.salon.models.ai.dto.AiConfigResponse;
import com.cristiane.salon.models.ai.service.AiConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiConfigController.class)
class AiConfigControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AiConfigService aiConfigService;

    @Test
    @WithMockUser(roles = { "SYSADMIN" })
    void get_returnsConfigWithoutPlainApiKey() throws Exception {
        AiConfigResponse response = new AiConfigResponse(
                "https://llm.rodrigor.com", "gpt-4o-mini", "sk-•••••WesymE", true,
                new BigDecimal("0.30"), 500, true, 200, "sysadmin@salao.com", LocalDateTime.now()
        );
        when(aiConfigService.get()).thenReturn(response);

        mvc.perform(get("/v1/sysadmin/ai-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model").value("gpt-4o-mini"))
                .andExpect(jsonPath("$.apiKeyMasked").value("sk-•••••WesymE"))
                .andExpect(jsonPath("$.apiKeyConfigured").value(true));
    }

    @Test
    @WithMockUser(roles = { "SYSADMIN" })
    void update_returnsUpdatedConfig() throws Exception {
        AiConfigResponse response = new AiConfigResponse(
                "https://llm.rodrigor.com", "gpt-4o", null, false,
                new BigDecimal("0.5"), 800, true, 300, "sysadmin@salao.com", LocalDateTime.now()
        );
        when(aiConfigService.update(any())).thenReturn(response);

        String body = """
                {
                  "baseUrl": "https://llm.rodrigor.com",
                  "model": "gpt-4o",
                  "apiKey": null,
                  "temperature": 0.5,
                  "maxTokens": 800,
                  "enabled": true,
                  "dailyCallBudget": 300
                }
                """;

        mvc.perform(put("/v1/sysadmin/ai-config")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model").value("gpt-4o"));
    }

    @Test
    @WithMockUser(roles = { "SYSADMIN" })
    void update_returns400_whenMissingRequiredFields() throws Exception {
        String body = "{}";

        mvc.perform(put("/v1/sysadmin/ai-config")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }
}
