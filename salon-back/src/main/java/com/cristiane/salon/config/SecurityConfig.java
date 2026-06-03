package com.cristiane.salon.config;

import com.cristiane.salon.security.AuditRequestFilter;
import com.cristiane.salon.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuditRequestFilter auditRequestFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<AuditRequestFilter> auditFilterRegistration(AuditRequestFilter filter) {
        FilterRegistrationBean<AuditRequestFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> {}) // will be configured in CorsConfig
            .authorizeHttpRequests(auth -> auth
                // Public routes
                .requestMatchers("/ping").permitAll()
                .requestMatchers("/v1/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/v1/services").permitAll()
                .requestMatchers(HttpMethod.GET, "/v1/products").permitAll()
                .requestMatchers(HttpMethod.GET, "/v1/employees/booking").permitAll()
                .requestMatchers(HttpMethod.GET, "/v1/feature-flags").permitAll()
                
                // Swagger & API Docs
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**").permitAll()
                
                // Static resources & Frontend SPA routing
                .requestMatchers(HttpMethod.GET, "/", "/index.html", "/static/**", "/assets/**", "/*.png", "/*.ico", "/*.json", "/*.txt").permitAll()
                
                // API routes require authentication (except the permitted ones above)
                .requestMatchers("/v1/**").authenticated()
                
                // Any other request (mostly frontend React routes) is permitted so the SPA config can forward to index.html
                .anyRequest().permitAll()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(auditRequestFilter, JwtAuthenticationFilter.class);

        return http.build();
    }
}
