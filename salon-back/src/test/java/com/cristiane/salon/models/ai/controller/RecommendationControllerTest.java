package com.cristiane.salon.models.ai.controller;

import com.cristiane.salon.controllers.BaseControllerTest;
import com.cristiane.salon.models.ai.dto.RecommendationItem;
import com.cristiane.salon.models.ai.dto.RecommendationResult;
import com.cristiane.salon.models.ai.entity.RecommendationPriority;
import com.cristiane.salon.models.ai.entity.RecommendationType;
import com.cristiane.salon.models.ai.service.RecommendationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecommendationController.class)
class RecommendationControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private RecommendationService recommendationService;

    @Test
    @WithMockUser(roles = { "ADMIN" })
    void getLatest_returnsCachedResult() throws Exception {
        RecommendationResult result = new RecommendationResult(
                RecommendationType.FINANCEIRO,
                List.of(new RecommendationItem("T", "D", "A", RecommendationPriority.ALTA)),
                LocalDateTime.now(),
                true
        );
        when(recommendationService.getLatestCached(RecommendationType.FINANCEIRO)).thenReturn(result);

        mvc.perform(get("/v1/admin/recommendations/FINANCEIRO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromCache").value(true))
                .andExpect(jsonPath("$.items[0].title").value("T"));
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    void generate_returnsFreshResult() throws Exception {
        RecommendationResult result = new RecommendationResult(
                RecommendationType.RETENCAO,
                List.of(new RecommendationItem("T2", "D2", "A2", RecommendationPriority.BAIXA)),
                LocalDateTime.now(),
                false
        );
        when(recommendationService.generate(eq(RecommendationType.RETENCAO), eq("USER"), any())).thenReturn(result);

        mvc.perform(post("/v1/admin/recommendations/RETENCAO/generate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromCache").value(false))
                .andExpect(jsonPath("$.items[0].title").value("T2"));
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    void getLatest_withInvalidType_returns400() throws Exception {
        mvc.perform(get("/v1/admin/recommendations/NAO_EXISTE"))
                .andExpect(status().isBadRequest());
    }
}
