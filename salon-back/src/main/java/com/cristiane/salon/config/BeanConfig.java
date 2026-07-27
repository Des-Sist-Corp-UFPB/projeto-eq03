package com.cristiane.salon.config;

import com.cristiane.salon.security.SecurityUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class BeanConfig {

    private final SecurityUserDetailsService userDetailsService;

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * ObjectMapper Jackson 2.x para os componentes que ainda dependem dele (AuditAspect,
     * PushService). A política de datas fica em {@link TimeConfig} — este bean só a registra,
     * para que os dois Jacksons (2.x aqui, 3.x nas respostas HTTP) nunca divirjam.
     */
    @Bean
    public ObjectMapper objectMapper(com.fasterxml.jackson.databind.Module salonJackson2TimeModule) {
        ObjectMapper mapper = new ObjectMapper();
        // Sem isso, qualquer tipo com LocalDate/LocalTime (ex.: StaffProfileRequest.birthDate)
        // faz este mapper falhar ao serializar — o que hoje é usado, entre outras coisas,
        // pelo AuditAspect para registrar os argumentos de métodos @Auditable(captureArgs=true).
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper.registerModule(salonJackson2TimeModule);
        return mapper;
    }
}