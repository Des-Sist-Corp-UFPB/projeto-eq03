package com.cristiane.salon.mcp.tools;

import com.cristiane.salon.models.ai.dto.RecommendationResult;
import com.cristiane.salon.models.ai.entity.RecommendationType;
import com.cristiane.salon.models.ai.service.RecommendationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationMcpToolsTest {

    @Mock
    private RecommendationService recommendationService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAsMcpToken(String tokenId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(tokenId, null, List.of(new SimpleGrantedAuthority("ROLE_MCP_CLIENT")))
        );
    }

    @Test
    void recomendacoesFinanceiras_delegatesToRecommendationServiceWithMcpTokenCaller() {
        authenticateAsMcpToken("7");
        RecommendationResult expected = new RecommendationResult(RecommendationType.FINANCEIRO, List.of(), LocalDateTime.now(), false);
        when(recommendationService.generate(RecommendationType.FINANCEIRO, "MCP_TOKEN", "7")).thenReturn(expected);

        RecommendationMcpTools tools = new RecommendationMcpTools(recommendationService);
        RecommendationResult result = tools.recomendacoesFinanceiras();

        assertThat(result).isEqualTo(expected);
        verify(recommendationService).generate(eq(RecommendationType.FINANCEIRO), eq("MCP_TOKEN"), eq("7"));
    }

    @Test
    void recomendacoesRetencaoClientes_delegatesToRecommendationServiceWithMcpTokenCaller() {
        authenticateAsMcpToken("9");
        RecommendationResult expected = new RecommendationResult(RecommendationType.RETENCAO, List.of(), LocalDateTime.now(), true);
        when(recommendationService.generate(RecommendationType.RETENCAO, "MCP_TOKEN", "9")).thenReturn(expected);

        RecommendationMcpTools tools = new RecommendationMcpTools(recommendationService);
        RecommendationResult result = tools.recomendacoesRetencaoClientes();

        assertThat(result).isEqualTo(expected);
        ArgumentCaptor<String> callerIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(recommendationService).generate(eq(RecommendationType.RETENCAO), eq("MCP_TOKEN"), callerIdCaptor.capture());
        assertThat(callerIdCaptor.getValue()).isEqualTo("9");
    }

    @Test
    void recomendacoesFinanceiras_withNoAuthentication_usesUnknownCallerId() {
        RecommendationResult expected = new RecommendationResult(RecommendationType.FINANCEIRO, List.of(), LocalDateTime.now(), false);
        when(recommendationService.generate(RecommendationType.FINANCEIRO, "MCP_TOKEN", "unknown")).thenReturn(expected);

        RecommendationMcpTools tools = new RecommendationMcpTools(recommendationService);
        tools.recomendacoesFinanceiras();

        verify(recommendationService).generate(eq(RecommendationType.FINANCEIRO), eq("MCP_TOKEN"), eq("unknown"));
    }
}
