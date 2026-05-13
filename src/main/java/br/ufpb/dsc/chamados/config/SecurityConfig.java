package br.ufpb.dsc.chamados.config;

import br.ufpb.dsc.chamados.security.SecurityFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuração de Segurança com JWT.
 *
 * Fluxo:
 * 1. POST /api/auth/login - Autentica e retorna JWT
 * 2. Cliente armazena token no header: Authorization: Bearer <token>
 * 3. SecurityFilter valida token em cada requisição
 * 4. Endpoints protegidos (GET /usuarios, etc.) requerem autenticação
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private SecurityFilter securityFilter;

    @Autowired
    private UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Desabilita proteção CSRF (necessário para APIs REST com JWT)
                .csrf(csrf -> csrf.disable())

                // Configura rotas públicas vs protegidas
                .authorizeHttpRequests(authz -> authz
                        // Públicas
                        .requestMatchers(                                "/",                                "/login",
                                "/api/auth/login",
                                "/actuator/health",
                                "/actuator/**",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/bootstrap/**",
                                "/webjars/**"
                        ).permitAll()
                        // Protegidas - require autenticação
                        .anyRequest().authenticated()
                )

                // Sem sessão (stateless) - cada requisição usa JWT
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Tratamento de erros de autenticação: redireciona para /login
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.sendRedirect("/login");
                        })
                )

                // Adiciona filtro JWT antes do filtro padrão
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * AuthenticationManager: responsável por autenticar usuários.
     * Usa UserDetailsService para buscar usuário + PasswordEncoder para validar senha.
     */
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder auth = http.getSharedObject(AuthenticationManagerBuilder.class);
        auth
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder());
        return auth.build();
    }

    /**
     * PasswordEncoder: BCrypt para hash seguro de senhas.
     * Com salt aleatório, tornando rainbow tables inúteis.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
