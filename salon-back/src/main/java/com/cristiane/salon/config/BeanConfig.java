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

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

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

    // Jackson 2.x ObjectMapper for legacy components dependency injection
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        com.fasterxml.jackson.databind.module.SimpleModule module = new com.fasterxml.jackson.databind.module.SimpleModule();
        
        module.addSerializer(LocalDateTime.class, new com.fasterxml.jackson.databind.JsonSerializer<LocalDateTime>() {
            private static final ZoneId UTC = ZoneId.of("UTC");
            private static final ZoneId RECIFE = ZoneId.of("America/Recife");
            private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

            @Override
            public void serialize(LocalDateTime value, com.fasterxml.jackson.core.JsonGenerator gen, com.fasterxml.jackson.databind.SerializerProvider serializers) throws IOException {
                if (value != null) {
                    ZonedDateTime utcDateTime = value.atZone(UTC);
                    ZonedDateTime recifeDateTime = utcDateTime.withZoneSameInstant(RECIFE);
                    gen.writeString(recifeDateTime.toLocalDateTime().format(FORMATTER));
                }
            }
        });

        module.addSerializer(Instant.class, new com.fasterxml.jackson.databind.JsonSerializer<Instant>() {
            private static final ZoneId RECIFE = ZoneId.of("America/Recife");
            private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

            @Override
            public void serialize(Instant value, com.fasterxml.jackson.core.JsonGenerator gen, com.fasterxml.jackson.databind.SerializerProvider serializers) throws IOException {
                if (value != null) {
                    ZonedDateTime recifeDateTime = value.atZone(RECIFE);
                    gen.writeString(recifeDateTime.format(FORMATTER));
                }
            }
        });
        
        mapper.registerModule(module);
        return mapper;
    }

    // Jackson 3.x JacksonModule to format java.time classes globally in Spring Boot 4.x
    @Bean
    public tools.jackson.databind.JacksonModule customTimeModule() {
        tools.jackson.databind.module.SimpleModule module = new tools.jackson.databind.module.SimpleModule();
        
        module.addSerializer(LocalDateTime.class, new tools.jackson.databind.ValueSerializer<LocalDateTime>() {
            private static final ZoneId UTC = ZoneId.of("UTC");
            private static final ZoneId RECIFE = ZoneId.of("America/Recife");
            private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

            @Override
            public void serialize(LocalDateTime value, tools.jackson.core.JsonGenerator gen, tools.jackson.databind.SerializationContext serializers) {
                if (value != null) {
                    ZonedDateTime utcDateTime = value.atZone(UTC);
                    ZonedDateTime recifeDateTime = utcDateTime.withZoneSameInstant(RECIFE);
                    gen.writeString(recifeDateTime.toLocalDateTime().format(FORMATTER));
                }
            }
        });

        module.addSerializer(Instant.class, new tools.jackson.databind.ValueSerializer<Instant>() {
            private static final ZoneId RECIFE = ZoneId.of("America/Recife");
            private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

            @Override
            public void serialize(Instant value, tools.jackson.core.JsonGenerator gen, tools.jackson.databind.SerializationContext serializers) {
                if (value != null) {
                    ZonedDateTime recifeDateTime = value.atZone(RECIFE);
                    gen.writeString(recifeDateTime.format(FORMATTER));
                }
            }
        });

        return module;
    }
}