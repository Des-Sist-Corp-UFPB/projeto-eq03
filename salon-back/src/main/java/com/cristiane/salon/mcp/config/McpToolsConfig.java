package com.cristiane.salon.mcp.config;

import com.cristiane.salon.mcp.tools.RecommendationMcpTools;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class McpToolsConfig {

    private final RecommendationMcpTools recommendationMcpTools;

    @Bean
    public ToolCallbackProvider recommendationToolCallbackProvider() {
        return MethodToolCallbackProvider.builder()
                .toolObjects(recommendationMcpTools)
                .build();
    }
}
