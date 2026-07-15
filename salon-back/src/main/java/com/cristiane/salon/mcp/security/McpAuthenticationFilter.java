package com.cristiane.salon.mcp.security;

import com.cristiane.salon.models.ai.entity.McpAccessToken;
import com.cristiane.salon.models.ai.service.McpTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Autentica chamadas ao servidor MCP via Bearer token próprio (prefixo "mcp_"), gerado e
 * gerenciado pela Central de IA — nunca herda as permissões de quem criou o token. A
 * autoridade concedida ({@code ROLE_MCP_CLIENT}) só existe pras tools MCP (ver
 * {@link com.cristiane.salon.config.SecurityConfig}), princípio do menor privilégio: se um
 * token vazar, o estrago fica limitado a "consultar recomendações", nunca a tudo que o
 * usuário que gerou o token poderia fazer.
 */
@Component
@RequiredArgsConstructor
public class McpAuthenticationFilter extends OncePerRequestFilter {

    private static final String TOKEN_PREFIX = "mcp_";

    private final McpTokenService mcpTokenService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer " + TOKEN_PREFIX)) {
            String rawToken = authHeader.substring(7);
            Optional<McpAccessToken> validated = mcpTokenService.validateAndTouch(rawToken);

            if (validated.isPresent() && SecurityContextHolder.getContext().getAuthentication() == null) {
                McpAccessToken token = validated.get();
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        String.valueOf(token.getId()),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_MCP_CLIENT"))
                );
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
