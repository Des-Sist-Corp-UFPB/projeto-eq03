package com.cristiane.salon.config;

import com.cristiane.salon.security.SecurityUserDetailsService;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
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
import java.util.TimeZone;

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

    @Bean
    public ObjectMapper objectMapper() {
        SimpleModule module = new SimpleModule();
        
        // Custom LocalDateTime serializer: assumes database/JVM LocalDateTime is UTC, converts to America/Recife
        module.addSerializer(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
            private static final ZoneId UTC = ZoneId.of("UTC");
            private static final ZoneId RECIFE = ZoneId.of("America/Recife");
            private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

            @Override
            public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                if (value != null) {
                    ZonedDateTime utcDateTime = value.atZone(UTC);
                    ZonedDateTime recifeDateTime = utcDateTime.withZoneSameInstant(RECIFE);
                    gen.writeString(recifeDateTime.toLocalDateTime().format(FORMATTER));
                }
            }
        });

        // Custom Instant serializer: converts Instant to America/Recife and formats
        module.addSerializer(Instant.class, new JsonSerializer<Instant>() {
            private static final ZoneId RECIFE = ZoneId.of("America/Recife");
            private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

            @Override
            public void serialize(Instant value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                if (value != null) {
                    ZonedDateTime recifeDateTime = value.atZone(RECIFE);
                    gen.writeString(recifeDateTime.format(FORMATTER));
                }
            }
        });

        return JsonMapper.builder()
                .findAndAddModules()
                .defaultTimeZone(TimeZone.getTimeZone("America/Recife"))
                .addModule(module)
                .build();
    }
}