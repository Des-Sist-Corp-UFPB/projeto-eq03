package br.ufpb.dsc.chamados.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro de segurança que valida tokens JWT em cada requisição.
 *
 * Intercepta todas as requisições, extrai o token do header Authorization,
 * valida e carrega os dados do usuário no SecurityContext.
 *
 * Fluxo:
 * 1. Extrai token do header "Authorization: Bearer <token>"
 * 2. Valida o token
 * 3. Carrega usuário do banco de dados
 * 4. Configura autenticação no SecurityContext
 * 5. Continua o processamento da requisição
 */
@Slf4j
@Component
public class SecurityFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        
        log.debug("SecurityFilter: Processando requisição {} {}", request.getMethod(), request.getRequestURI());

        try {
            // 1. Extrai o token do header Authorization primeiro
            String token = extractToken(authHeader);
            log.debug("Token do header Authorization: {}", token != null ? "encontrado" : "não encontrado");
            
            // 2. Se não encontrou no header, tenta extrair do cookie
            if (token == null) {
                token = extractTokenFromCookie(request);
                log.debug("Token do cookie: {}", token != null ? "encontrado" : "não encontrado");
            }

            if (token != null) {
                // 3. Valida e extrai ID do usuário do token
                Long userId = tokenProvider.getUserIdFromToken(token);
                String matricula = tokenProvider.getMatriculaFromToken(token);

                log.debug("Token válido para usuário ID: {}, Matrícula: {}", userId, matricula);

                // 4. Carrega dados do usuário do banco
                UserDetails userDetails = userDetailsService.loadUserByUsername(matricula);
                log.debug("UserDetails carregado para: {}", matricula);

                // 5. Cria autenticação e configura no contexto
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("Autenticação configurada para usuário: {}", matricula);
            } else {
                log.debug("Nenhum token encontrado em Authorization header ou cookie");
            }
        } catch (Exception e) {
            log.debug("Erro ao processar token JWT: {} - Exception: {}", e.getMessage(), e.getClass().getSimpleName());
            log.debug("Stack trace: ", e);
            // Continua sem autenticação - as anotações @Secured cuidam do acesso
        }

        // 6. Continua o processamento
        filterChain.doFilter(request, response);
    }

    /**
     * Extrai o token do header Authorization.
     * Formato: "Bearer <token>"
     */
    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }

    /**
     * Extrai o token de um cookie chamado 'authToken'.
     * Usado por clientes JavaScript que armazenam o token em localStorage/cookie.
     */
    private String extractTokenFromCookie(HttpServletRequest request) {
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        
        for (jakarta.servlet.http.Cookie cookie : cookies) {
            if ("authToken".equals(cookie.getName())) {
                String value = cookie.getValue();
                return value != null && !value.isEmpty() ? value : null;
            }
        }
        return null;
    }
}
