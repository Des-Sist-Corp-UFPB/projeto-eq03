package com.cristiane.salon.mcp.tools;

import com.cristiane.salon.models.ai.dto.RecommendationResult;
import com.cristiane.salon.models.ai.entity.RecommendationType;
import com.cristiane.salon.models.ai.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Casca fina de protocolo MCP sobre o {@link RecommendationService} — mesma lógica de negócio
 * que a tela de Recomendações (REST) usa, nenhuma regra duplicada aqui. Quem chama é um cliente
 * MCP (Claude Desktop, Cursor etc.) autenticado via {@link com.cristiane.salon.mcp.security.McpAuthenticationFilter}.
 */
@Service
@RequiredArgsConstructor
public class RecommendationMcpTools {

    private final RecommendationService recommendationService;

    @Tool(
            name = "recomendacoes_financeiras",
            description = "Retorna recomendações de negócio sobre faturamento, ocupação de horários e "
                    + "distribuição de agendamentos entre profissionais do salão, com base nos últimos 30 dias."
    )
    public RecommendationResult recomendacoesFinanceiras() {
        return recommendationService.generate(RecommendationType.FINANCEIRO, "MCP_TOKEN", currentTokenId());
    }

    @Tool(
            name = "recomendacoes_retencao_clientes",
            description = "Retorna recomendações de retenção de clientes, identificando quem não agenda um "
                    + "serviço há bastante tempo e sugerindo ações de reengajamento."
    )
    public RecommendationResult recomendacoesRetencaoClientes() {
        return recommendationService.generate(RecommendationType.RETENCAO, "MCP_TOKEN", currentTokenId());
    }

    private String currentTokenId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "unknown";
    }
}
